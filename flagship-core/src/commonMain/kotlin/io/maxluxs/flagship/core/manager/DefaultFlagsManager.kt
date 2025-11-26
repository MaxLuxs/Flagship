package io.maxluxs.flagship.core.manager

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.analytics.ExposureTracker
import io.maxluxs.flagship.core.analytics.ProviderAnalyticsReporter
import io.maxluxs.flagship.core.analytics.ProviderAnalyticsConfig
import io.maxluxs.flagship.core.evaluator.FlagsEvaluator
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.BaseFlagsProvider
import io.maxluxs.flagship.core.provider.LazyFlagsProvider
import io.maxluxs.flagship.core.provider.isLazyProvider
import io.maxluxs.flagship.core.provider.ProviderMetricsTracker
import io.maxluxs.flagship.core.provider.RealtimeFlagsProvider
import io.maxluxs.flagship.core.realtime.RealtimeManager
import io.maxluxs.flagship.core.security.SignatureValidator
import io.maxluxs.flagship.core.performance.PerformanceProfiler
import io.maxluxs.flagship.core.performance.MemoryOptimizer
import io.maxluxs.flagship.core.performance.OperationStats
import io.maxluxs.flagship.core.performance.MemoryUsage
import io.maxluxs.flagship.core.util.currentTimeMs
import io.maxluxs.flagship.core.util.ValueConverter
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

class DefaultFlagsManager(
    private val config: FlagsConfig,
    private val coroutineContext: CoroutineContext = Dispatchers.Default
) : FlagsManager {
    private val scope = CoroutineScope(coroutineContext + SupervisorJob())
    private val evaluator = FlagsEvaluator()
    
    private val snapshots = mutableMapOf<String, ProviderSnapshot>()
    private val overrides = mutableMapOf<FlagKey, FlagValue>()
    private val listeners = mutableListOf<FlagsListener>()
    
    private val mutex = Mutex()
    private val exposureTracker = ExposureTracker()
    
    private var defaultContext: EvalContext? = null
    private var bootstrapped = false
    private val bootstrapMutex = Mutex()
    
    private val realtimeManager: RealtimeManager? = if (config.enableRealtime) {
        RealtimeManager(this, scope, coroutineContext)
    } else {
        null
    }
    
    private val signatureValidator: SignatureValidator? = config.crypto?.let {
        SignatureValidator(it, config.serializer)
    }
    
    private val snapshotVerifier: SnapshotVerifier? = signatureValidator?.let {
        SnapshotVerifier(it, config.logger)
    }
    
    // Provider metrics tracking
    private val metricsTracker: ProviderMetricsTracker? = if (config.providerAnalytics?.enabled == true) {
        ProviderMetricsTracker(config.clock)
    } else {
        null
    }
    
    // Performance profiling
    private val profiler: PerformanceProfiler = PerformanceProfiler(
        logger = config.logger,
        enabled = config.enablePerformanceProfiling ?: false
    )
    
    // Memory optimization
    private val memoryOptimizer: MemoryOptimizer = MemoryOptimizer(
        logger = config.logger,
        maxSnapshots = config.maxSnapshots ?: 10,
        maxSnapshotAgeMs = config.maxSnapshotAgeMs ?: (24 * 60 * 60 * 1000)
    )
    
    // Lazy provider loading
    private val lazyProviders = mutableSetOf<String>()
    private val lazyProviderMutex = Mutex()
    private var allLazyProvidersLoaded = false
    
    private val analyticsReporter: ProviderAnalyticsReporter? = config.providerAnalytics?.let { analyticsConfig ->
        if (analyticsConfig.enabled && metricsTracker != null) {
            // Extract analytics URL from REST provider if not provided
            val analyticsUrl = analyticsConfig.analyticsUrl ?: run {
                config.providers.firstOrNull { 
                    it::class.simpleName == "RestFlagsProvider" 
                }?.let { provider ->
                    // Try to extract baseUrl from REST provider
                    // For now, we'll require analyticsUrl to be provided explicitly
                    // In the future, we can add a method to FlagsProvider to get base URL
                    null
                } ?: ""
            }
            
            if (analyticsUrl.isBlank()) {
                config.logger.warn("FlagsManager", "Provider analytics enabled but analyticsUrl is not set. Metrics will not be sent.")
                null
            } else {
                val reporter = ProviderAnalyticsReporter(
                    projectId = analyticsConfig.projectId,
                    analyticsUrl = analyticsUrl,
                    apiKey = analyticsConfig.apiKey,
                    metricsTracker = metricsTracker,
                    reportingIntervalMs = analyticsConfig.reportingIntervalMs,
                    batchSize = analyticsConfig.batchSize,
                    maxRetries = analyticsConfig.maxRetries,
                    logger = config.logger,
                    clock = config.clock
                )
                reporter.start()
                reporter
            }
        } else {
            null
        }
    }

    fun setDefaultContext(context: EvalContext) {
        defaultContext = context
    }

    override suspend fun isEnabled(key: FlagKey, default: Boolean, ctx: EvalContext?): Boolean {
        return profiler.measure("isEnabled") {
            val context = ctx ?: defaultContext ?: return@measure default
            ensureLazyProvidersLoaded(key)
            val value = evaluateInternal(key, FlagValue.Bool(default))
            value?.asBoolean() ?: default
        }
    }
    
    /**
     * Ensure lazy providers are loaded when a flag is accessed.
     * 
     * Optimized with early exit if all lazy providers are already loaded.
     */
    private suspend fun ensureLazyProvidersLoaded(key: FlagKey) {
        // Fast path: if all lazy providers are loaded, skip the check
        if (allLazyProvidersLoaded) return
        
        lazyProviderMutex.withLock {
            // Double-check after acquiring lock
            if (allLazyProvidersLoaded) return
            
            val providersToLoad = config.providers.filter { provider ->
                provider.isLazyProvider() && 
                !lazyProviders.contains(provider.name) &&
                snapshots[provider.name] == null
            }
            
            if (providersToLoad.isEmpty()) {
                allLazyProvidersLoaded = true
                return
            }
            
            coroutineScope {
                providersToLoad.forEach { provider ->
                    launch {
                        try {
                            val snapshot = provider.bootstrap()
                            mutex.withLock {
                                snapshots[provider.name] = snapshot
                            }
                            config.cache.save(provider.name, snapshot)
                            lazyProviderMutex.withLock {
                                lazyProviders.add(provider.name)
                            }
                            config.logger.info("FlagsManager", "Lazy provider ${provider.name} loaded on-demand for flag $key")
                        } catch (e: Exception) {
                            config.logger.error("FlagsManager", "Failed to load lazy provider ${provider.name}", e)
                        }
                    }
                }
            }
            
            // Check if all lazy providers are now loaded
            val allLazyProviders = config.providers.filter { it.isLazyProvider() }
            if (allLazyProviders.all { lazyProviders.contains(it.name) }) {
                allLazyProvidersLoaded = true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> value(key: FlagKey, default: T, ctx: EvalContext?): T {
        return profiler.measure("value") {
            val context = ctx ?: defaultContext ?: return@measure default
            ensureLazyProvidersLoaded(key)
            val flagDefault = ValueConverter.toFlagValue(default) ?: return@measure default
            val value = evaluateInternal(key, flagDefault)
            ValueConverter.fromFlagValue(value, default)
        }
    }

    override suspend fun assign(key: ExperimentKey, ctx: EvalContext?): ExperimentAssignment? {
        val context = ctx ?: defaultContext ?: return null
        // Snapshots are read-only list copies in evaluateExperiment, so no lock needed there if passed correctly
        // But we need lock to get snapshots values safely
        // Preserve provider order by iterating config.providers
        val currentSnapshots = mutex.withLock { 
            config.providers.mapNotNull { provider ->
                snapshots[provider.name]
            }
        }
        
        val assignment = evaluator.evaluateExperiment(key, context, currentSnapshots)
        
        if (assignment != null) {
            exposureTracker.trackIfNeeded(
                experimentKey = key,
                variant = assignment.variant,
                analytics = config.analytics
            )
            // Notify listeners about experiment assignment
            notifyExperimentAssigned(key, assignment)
        }
        
        return assignment
    }

    override fun assignSync(key: ExperimentKey, ctx: EvalContext?): ExperimentAssignment? {
        if (!bootstrapped) {
            throw IllegalStateException("Bootstrap not completed. Call ensureBootstrap() first or use assign() instead.")
        }
        
        val context = ctx ?: defaultContext ?: return null
        // For sync methods, we need to access data without suspending
        // This is safe because we only read immutable snapshots after bootstrap
        val currentSnapshots = config.providers.mapNotNull { provider ->
            snapshots[provider.name]
        }
        
        val assignment = evaluator.evaluateExperiment(key, context, currentSnapshots)
        
        if (assignment != null) {
            exposureTracker.trackIfNeeded(
                experimentKey = key,
                variant = assignment.variant,
                analytics = config.analytics
            )
            // Notify listeners about experiment assignment (async, but don't block)
            notifyExperimentAssigned(key, assignment)
        }
        
        return assignment
    }

    override suspend fun ensureBootstrap(timeoutMs: Long): Boolean {
        if (bootstrapped) return true
        
        return withTimeoutOrNull(timeoutMs) {
            bootstrapInternal()
            true
        } ?: false
    }

    override fun refresh(force: Boolean, onProgress: ((String, Boolean) -> Unit)?) {
        scope.launch {
            refreshInternal(force, onProgress)
        }
    }
    
    override fun isEnabledSync(key: FlagKey, default: Boolean, ctx: EvalContext?): Boolean {
        if (!bootstrapped) {
            throw IllegalStateException("Bootstrap not completed. Call ensureBootstrap() first or use isEnabled() instead.")
        }
        
        val context = ctx ?: defaultContext ?: return default
        val value = evaluateInternalSync(key, FlagValue.Bool(default))
        return value?.asBoolean() ?: default
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T> valueSync(key: FlagKey, default: T, ctx: EvalContext?): T {
        if (!bootstrapped) {
            throw IllegalStateException("Bootstrap not completed. Call ensureBootstrap() first or use value() instead.")
        }
        
        val context = ctx ?: defaultContext ?: return default
        val flagDefault = ValueConverter.toFlagValue(default) ?: return default
        val value = evaluateInternalSync(key, flagDefault)
        return ValueConverter.fromFlagValue(value, default)
    }
    
    private fun evaluateInternalSync(key: FlagKey, default: FlagValue?): FlagValue? {
        // For sync methods, we need to access data without suspending
        // This is safe because we only read immutable snapshots
        // Note: This is a simplified sync version - for full sync support, use isEnabledSync()
        val currentOverrides = overrides.toMap()
        val orderedSnapshots = config.providers.mapNotNull { provider ->
            snapshots[provider.name]
        }
        
        return evaluator.evaluateFlag(
            key = key,
            overrides = currentOverrides,
            snapshots = orderedSnapshots,
            default = default
        )
    }

    override fun addListener(listener: FlagsListener) {
        scope.launch {
            mutex.withLock {
                listeners.add(listener)
            }
        }
    }

    override fun removeListener(listener: FlagsListener) {
        scope.launch {
            mutex.withLock {
                listeners.remove(listener)
            }
        }
    }

    override fun setOverride(key: FlagKey, value: FlagValue) {
        scope.launch {
            mutex.withLock {
                overrides[key] = value
            }
            notifyOverrideChanged(key)
        }
    }

    override fun clearOverride(key: FlagKey) {
        scope.launch {
            mutex.withLock {
                overrides.remove(key)
            }
            notifyOverrideChanged(key)
        }
    }

    override suspend fun listOverrides(): Map<FlagKey, FlagValue> {
        return mutex.withLock { overrides.toMap() }
    }
    
    override suspend fun listAllFlags(): Map<FlagKey, FlagValue> {
        return mutex.withLock {
            val allFlags = mutableMapOf<FlagKey, FlagValue>()
            snapshots.values.forEach { snapshot ->
                allFlags.putAll(snapshot.flags)
            }
            allFlags.toMap()
        }
    }

    private suspend fun evaluateInternal(key: FlagKey, default: FlagValue?): FlagValue? {
        val (currentOverrides, currentSnapshots) = mutex.withLock {
            // Preserve provider order by iterating config.providers
            val orderedSnapshots = config.providers.mapNotNull { provider ->
                snapshots[provider.name]
            }
            overrides.toMap() to orderedSnapshots
        }
        
        return evaluator.evaluateFlag(
            key = key,
            overrides = currentOverrides,
            snapshots = currentSnapshots,
            default = default
        )
    }
    
    override suspend fun getFlagStatus(key: FlagKey): FlagStatus {
        return try {
            val (currentOverrides, currentSnapshots, providerMap) = mutex.withLock {
                val orderedSnapshots = config.providers.mapNotNull { provider ->
                    snapshots[provider.name]?.let { snapshot ->
                        provider.name to snapshot
                    }
                }
                val providerMap = orderedSnapshots.associate { it.first to it.second }
                Triple(overrides.toMap(), orderedSnapshots.map { it.second }, providerMap)
            }
            
            // Check overrides first (highest precedence)
            if (currentOverrides.containsKey(key)) {
                return FlagStatus(
                    exists = true,
                    source = FlagSource.OVERRIDE,
                    lastError = null,
                    lastUpdated = null,
                    providerName = null
                )
            }
            
            // Check snapshots in provider order
            val currentTime = config.clock.currentTimeMs()
            for ((providerName, snapshot) in providerMap.entries) {
                val flagValue = snapshot.flags[key]
                if (flagValue != null) {
                    // Determine if snapshot is fresh or cached
                    val isFresh = snapshot.ttlMs?.let { ttl ->
                        (currentTime - snapshot.fetchedAtMs) < ttl
                    } ?: true // If no TTL, consider it fresh
                    
                    val source = if (isFresh) FlagSource.PROVIDER else FlagSource.CACHE
                    
                    return FlagStatus(
                        exists = true,
                        source = source,
                        lastError = null,
                        lastUpdated = snapshot.fetchedAtMs,
                        providerName = providerName
                    )
                }
            }
            
            // Flag not found - using default
            FlagStatus(
                exists = false,
                source = FlagSource.DEFAULT,
                lastError = null,
                lastUpdated = null,
                providerName = null
            )
        } catch (e: Exception) {
            FlagStatus(
                exists = false,
                source = FlagSource.DEFAULT,
                lastError = e.message ?: e.toString(),
                lastUpdated = null,
                providerName = null
            )
        }
    }
    
    override suspend fun evaluateFlags(
        keys: List<FlagKey>,
        defaults: Map<FlagKey, FlagValue>,
        ctx: EvalContext?
    ): Map<FlagKey, FlagValue> {
        return profiler.measure("evaluateFlags") {
            val context = ctx ?: defaultContext ?: return@measure keys.associateWith { defaults[it] ?: FlagValue.Bool(false) }
            
            // Ensure lazy providers are loaded for all keys
            keys.forEach { key ->
                ensureLazyProvidersLoaded(key)
            }
            
            val (currentOverrides, currentSnapshots) = mutex.withLock {
                val orderedSnapshots = config.providers.mapNotNull { provider ->
                    snapshots[provider.name]
                }
                overrides.toMap() to orderedSnapshots
            }
            
            // Evaluate all flags in a single pass
            keys.associateWith { key ->
                val default = defaults[key]
                evaluator.evaluateFlag(
                    key = key,
                    overrides = currentOverrides,
                    snapshots = currentSnapshots,
                    default = default
                ) ?: default ?: FlagValue.Bool(false)
            }
        }
    }
    
    override suspend fun evaluateExperiments(
        keys: List<ExperimentKey>,
        ctx: EvalContext?
    ): Map<ExperimentKey, ExperimentAssignment?> {
        return profiler.measure("evaluateExperiments") {
            val context = ctx ?: defaultContext ?: return@measure keys.associateWith { null }
            
            val currentSnapshots = mutex.withLock {
                config.providers.mapNotNull { provider ->
                    snapshots[provider.name]
                }
            }
            
            // Evaluate all experiments in a single pass
            keys.associateWith { key ->
                evaluator.evaluateExperiment(
                    key = key,
                    context = context,
                    snapshots = currentSnapshots
                )
            }
        }
    }
    
    override suspend fun preload(keys: List<FlagKey>) {
        profiler.measure<Unit>("preload") {
            // Ensure lazy providers are loaded
            keys.forEach { key ->
                ensureLazyProvidersLoaded(key)
            }
            
            // Evaluate flags to ensure they are in cache
            val defaults = keys.associateWith { FlagValue.Bool(false) }
            evaluateFlags(keys, defaults)
        }
    }
    
    override suspend fun preloadForUser(userId: String, keys: List<FlagKey>) {
        profiler.measure<Unit>("preloadForUser") {
            val userContext = defaultContext?.copy(userId = userId) ?: EvalContext(
                userId = userId,
                deviceId = defaultContext?.deviceId ?: "",
                appVersion = defaultContext?.appVersion ?: "",
                osName = defaultContext?.osName ?: "",
                osVersion = defaultContext?.osVersion ?: ""
            )
            
            // Ensure lazy providers are loaded
            keys.forEach { key ->
                ensureLazyProvidersLoaded(key)
            }
            
            // Evaluate flags with user context to ensure they are in cache
            val defaults = keys.associateWith { FlagValue.Bool(false) }
            evaluateFlags(keys, defaults, userContext)
        }
    }

    private suspend fun bootstrapInternal() {
        bootstrapMutex.withLock {
            if (bootstrapped) return
            
            config.logger.info("FlagsManager", "Starting bootstrap...")
            
            // Load from cache first
            val previousSnapshots = mutex.withLock { snapshots.toMap() }
            // Load from cache
            config.providers.forEach { provider ->
                try {
                    val cachedSnapshot = config.cache.load(provider.name)
                    if (cachedSnapshot != null) {
                        mutex.withLock {
                            snapshots[provider.name] = cachedSnapshot
                        }
                        config.logger.info("FlagsManager", "Loaded from cache: ${provider.name}")
                    }
                } catch (e: Exception) {
                    config.logger.error("FlagsManager", "Failed to load cache for ${provider.name}", e)
                }
            }
            
            // Pass metricsTracker to providers that support it (if available)
            if (metricsTracker != null) {
                config.providers.forEach { provider ->
                    if (provider is BaseFlagsProvider) {
                        provider.setMetricsTracker(metricsTracker)
                    }
                }
            }
            
            // Separate eager and lazy providers
            val (eagerProviders, lazyProvidersList) = config.providers.partition { provider ->
                !provider.isLazyProvider()
            }
            
            // Mark lazy providers
            lazyProviderMutex.withLock {
                lazyProviders.addAll(lazyProvidersList.map { it.name })
            }
            
            // Fetch from eager providers only
            coroutineScope {
                val results = eagerProviders.map { provider ->
                    async {
                        profiler.measure("bootstrap.${provider.name}") {
                            try {
                                val snapshot = provider.bootstrap()
                                
                                // Verify signature if present
                                snapshotVerifier?.verifyOrThrow(snapshot, provider.name)
                                
                                val previousSnapshot = mutex.withLock {
                                    val prev = snapshots[provider.name]
                                    snapshots[provider.name] = snapshot
                                    prev
                                }
                                
                                // Detect flag changes
                                if (previousSnapshot != null) {
                                    detectFlagChanges(provider.name, previousSnapshot, snapshot)
                                }
                                
                                config.cache.save(provider.name, snapshot)
                                config.logger.info("FlagsManager", "Bootstrapped from ${provider.name}")
                                notifyProviderUpdated(provider.name, true)
                                provider.name to snapshot
                            } catch (e: Exception) {
                                config.logger.error("FlagsManager", "Bootstrap failed for ${provider.name}", e)
                                notifyProviderUpdated(provider.name, false)
                                null
                            }
                        }
                    }
                }
                
                results.awaitAll()
                
                // Log lazy providers
                if (lazyProvidersList.isNotEmpty()) {
                    config.logger.info("FlagsManager", "Lazy providers (${lazyProvidersList.size}) will be loaded on-demand: ${lazyProvidersList.map { it.name }.joinToString()}")
                }
            }
            
            // Optimize memory
            mutex.withLock {
                val cleaned = memoryOptimizer.cleanupSnapshots(snapshots)
                snapshots.clear()
                snapshots.putAll(cleaned)
            }
            bootstrapped = true
            notifySnapshotUpdated("bootstrap")
            
            // Connect realtime providers if enabled
            if (config.enableRealtime && realtimeManager != null) {
                config.providers.forEach { provider ->
                    if (provider is RealtimeFlagsProvider) {
                        realtimeManager.connect(provider)
                        config.logger.info("FlagsManager", "Connected realtime provider: ${provider.name}")
                    }
                }
            }
            
            // Log provider analytics status
            if (analyticsReporter != null) {
                config.logger.info("FlagsManager", "Provider analytics enabled for project ${config.providerAnalytics?.projectId}")
            }
            
            // Log performance profiling status
            if (config.enablePerformanceProfiling == true) {
                config.logger.info("FlagsManager", "Performance profiling enabled")
            }
            
            // Log memory optimization status
            config.logger.info("FlagsManager", "Memory optimization: maxSnapshots=${config.maxSnapshots ?: 10}, maxAge=${config.maxSnapshotAgeMs ?: (24 * 60 * 60 * 1000)}ms")
        }
    }
    
    /**
     * Get performance profiler for accessing metrics.
     */
    fun getProfiler(): PerformanceProfiler = profiler
    
    /**
     * Get memory optimizer for memory management.
     */
    fun getMemoryOptimizer(): MemoryOptimizer = memoryOptimizer
    
    /**
     * Optimize memory usage by cleaning up old snapshots.
     */
    suspend fun optimizeMemory() {
        mutex.withLock {
            val cleaned = memoryOptimizer.cleanupSnapshots(snapshots)
            snapshots.clear()
            snapshots.putAll(cleaned)
        }
        
        val suggestions = memoryOptimizer.suggestOptimizations(snapshots)
        if (suggestions.isNotEmpty()) {
            config.logger.warn("FlagsManager", "Memory optimization suggestions:\n${suggestions.joinToString("\n")}")
        }
    }
    
    /**
     * Get performance report.
     */
    suspend fun getPerformanceReport(): Map<String, OperationStats> {
        return profiler.getAllMetrics()
    }
    
    /**
     * Get memory usage statistics.
     */
    suspend fun getMemoryUsage(): MemoryUsage {
        return mutex.withLock {
            memoryOptimizer.estimateMemoryUsage(snapshots)
        }
    }

    private suspend fun refreshInternal(force: Boolean = false, onProgress: ((String, Boolean) -> Unit)? = null) {
        profiler.measure<Unit>("refresh") {
            config.logger.info("FlagsManager", "Starting refresh (force=$force)...")
            
            coroutineScope<Unit> {
                val results: List<Deferred<Pair<String, ProviderSnapshot>?>> = config.providers.map { provider ->
                    async {
                        profiler.measure<Pair<String, ProviderSnapshot>?>("refresh.${provider.name}") {
                            try {
                            // Skip if not forced and snapshot is recent (within last 30 seconds)
                            if (!force) {
                                val currentSnapshot = mutex.withLock { snapshots[provider.name] }
                                if (currentSnapshot != null) {
                                    val age = config.clock.currentTimeMs() - currentSnapshot.fetchedAtMs
                                    if (age < 30_000) { // 30 seconds
                                        config.logger.info("FlagsManager", "Skipping refresh for ${provider.name} (recent data)")
                                        onProgress?.invoke(provider.name, true)
                                        return@measure provider.name to currentSnapshot
                                    }
                                }
                            }
                            
                            val previousSnapshot = mutex.withLock { snapshots[provider.name] }
                            val snapshot = provider.refresh()
                            
                            // Verify signature if present
                            snapshotVerifier?.verifyOrThrow(snapshot, provider.name)
                            
                            mutex.withLock {
                                snapshots[provider.name] = snapshot
                            }
                            
                            // Detect flag changes
                            if (previousSnapshot != null) {
                                detectFlagChanges(provider.name, previousSnapshot, snapshot)
                            }
                            
                            config.cache.save(provider.name, snapshot)
                            config.logger.info("FlagsManager", "Refreshed from ${provider.name}")
                            notifyProviderUpdated(provider.name, true)
                            onProgress?.invoke(provider.name, true)
                            provider.name to snapshot
                        } catch (e: Exception) {
                            config.logger.error("FlagsManager", "Refresh failed for ${provider.name}", e)
                            notifyProviderUpdated(provider.name, false)
                            onProgress?.invoke(provider.name, false)
                            null
                            }
                        }
                    }
                }
                
                results.awaitAll()
            }
            notifySnapshotUpdated("refresh")
        }
    }
    
    suspend fun detectFlagChanges(
        providerName: String,
        previousSnapshot: ProviderSnapshot?,
        newSnapshot: ProviderSnapshot
    ) {
        if (previousSnapshot == null) return
        
        val previousFlags = previousSnapshot.flags
        val newFlags = newSnapshot.flags
        
        // Find changed flags
        val allKeys = (previousFlags.keys + newFlags.keys).toSet()
        allKeys.forEach { key ->
            val oldValue = previousFlags[key]
            val newValue = newFlags[key]
            
            if (oldValue != newValue) {
                notifyFlagChanged(key, oldValue, newValue)
            }
        }
    }

    private suspend fun loadFromCache() {
        config.providers.forEach { provider ->
            try {
                config.cache.load(provider.name)?.let { snapshot ->
                    // Verify signature if present
                    if (!(snapshotVerifier?.verify(snapshot, provider.name) ?: true)) {
                        config.logger.warn("FlagsManager", "Invalid signature in cache for ${provider.name}, ignoring")
                        return@let
                    }
                    
                    mutex.withLock {
                        snapshots[provider.name] = snapshot
                    }
                    config.logger.info("FlagsManager", "Loaded from cache: ${provider.name}")
                }
            } catch (e: Exception) {
                config.logger.error("FlagsManager", "Failed to load cache for ${provider.name}", e)
            }
        }
    }

    /**
     * Common method for notifying listeners.
     * 
     * Optimizes listener notification by copying the list once and then iterating.
     * This reduces mutex contention compared to individual locks.
     */
    private fun notifyListeners(action: (FlagsListener) -> Unit) {
        scope.launch {
            val listenersCopy = mutex.withLock { listeners.toList() }
            listenersCopy.forEach(action)
        }
    }

    private fun notifySnapshotUpdated(source: String) {
        notifyListeners { it.onSnapshotUpdated(source) }
    }

    private fun notifyOverrideChanged(key: FlagKey) {
        notifyListeners { it.onOverrideChanged(key) }
    }
    
    private fun notifyFlagChanged(key: FlagKey, oldValue: FlagValue?, newValue: FlagValue?) {
        notifyListeners { it.onFlagChanged(key, oldValue, newValue) }
    }
    
    private fun notifyExperimentAssigned(experimentKey: ExperimentKey, assignment: ExperimentAssignment) {
        notifyListeners { it.onExperimentAssigned(experimentKey, assignment) }
    }
    
    private fun notifyProviderUpdated(providerName: String, isHealthy: Boolean) {
        notifyListeners { it.onProviderUpdated(providerName, isHealthy) }
    }
    
    /**
     * Update snapshot from realtime provider.
     * Internal method for RealtimeManager to update snapshots.
     */
    suspend fun updateSnapshotFromRealtime(providerName: String, snapshot: ProviderSnapshot) {
        val previousSnapshot = mutex.withLock {
            val prev = snapshots[providerName]
            snapshots[providerName] = snapshot
            prev
        }
        
        // Detect flag changes
        detectFlagChanges(providerName, previousSnapshot, snapshot)
        
        config.cache.save(providerName, snapshot)
        config.logger.info("FlagsManager", "Updated from realtime: $providerName")
        notifySnapshotUpdated("realtime")
        notifyProviderUpdated(providerName, isHealthy = true)
    }
}

