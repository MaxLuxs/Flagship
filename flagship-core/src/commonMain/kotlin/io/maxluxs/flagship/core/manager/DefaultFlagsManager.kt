package io.maxluxs.flagship.core.manager

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.analytics.ExposureTracker
import io.maxluxs.flagship.core.evaluator.FlagsEvaluator
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.RealtimeFlagsProvider
import io.maxluxs.flagship.core.realtime.RealtimeManager
import io.maxluxs.flagship.core.security.SignatureValidator
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

    fun setDefaultContext(context: EvalContext) {
        defaultContext = context
    }

    override suspend fun isEnabled(key: FlagKey, default: Boolean, ctx: EvalContext?): Boolean {
        val context = ctx ?: defaultContext ?: return default
        val value = evaluateInternal(key, FlagValue.Bool(default))
        return value?.asBoolean() ?: default
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> value(key: FlagKey, default: T, ctx: EvalContext?): T {
        val context = ctx ?: defaultContext ?: return default
        val flagDefault = when (default) {
            is Boolean -> FlagValue.Bool(default)
            is Int -> FlagValue.Int(default)
            is Double -> FlagValue.Double(default)
            is String -> FlagValue.StringV(default)
            else -> return default
        }
        
        val value = evaluateInternal(key, flagDefault)
        return when (default) {
            is Boolean -> (value?.asBoolean() ?: default) as T
            is Int -> (value?.asInt() ?: default) as T
            is Double -> (value?.asDouble() ?: default) as T
            is String -> (value?.asString() ?: default) as T
            else -> default
        }
    }

    override suspend fun assign(key: ExperimentKey, ctx: EvalContext?): ExperimentAssignment? {
        val context = ctx ?: defaultContext ?: return null
        // Snapshots are read-only list copies in evaluateExperiment, so no lock needed there if passed correctly
        // But we need lock to get snapshots values safely
        val currentSnapshots = mutex.withLock { 
            snapshots.values.toList() 
        }
        
        val assignment = evaluator.evaluateExperiment(key, context, currentSnapshots)
        
        if (assignment != null) {
            exposureTracker.trackIfNeeded(
                experimentKey = key,
                variant = assignment.variant,
                analytics = config.analytics
            )
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

    override fun refresh() {
        scope.launch {
            refreshInternal()
        }
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
            overrides.toMap() to snapshots.values.toList()
        }
        
        return evaluator.evaluateFlag(
            key = key,
            overrides = currentOverrides,
            snapshots = currentSnapshots,
            default = default
        )
    }

    private suspend fun bootstrapInternal() {
        bootstrapMutex.withLock {
            if (bootstrapped) return
            
            config.logger.info("FlagsManager", "Starting bootstrap...")
            
            // Load from cache first
            loadFromCache()
            
            // Fetch from providers
            coroutineScope {
                val results = config.providers.map { provider ->
                    async {
                        try {
                            val snapshot = provider.bootstrap()
                            
                            // Verify signature if present
                            snapshotVerifier?.verifyOrThrow(snapshot, provider.name)
                            
                            mutex.withLock {
                                snapshots[provider.name] = snapshot
                            }
                            config.cache.save(provider.name, snapshot)
                            config.logger.info("FlagsManager", "Bootstrapped from ${provider.name}")
                            provider.name to snapshot
                        } catch (e: Exception) {
                            config.logger.error("FlagsManager", "Bootstrap failed for ${provider.name}", e)
                            null
                        }
                    }
                }
                
                results.awaitAll()
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
        }
    }

    private suspend fun refreshInternal() {
        config.logger.info("FlagsManager", "Starting refresh...")
        
        coroutineScope {
            val results = config.providers.map { provider ->
                    async {
                        try {
                            val snapshot = provider.refresh()
                            
                            // Verify signature if present
                            snapshotVerifier?.verifyOrThrow(snapshot, provider.name)
                            
                            mutex.withLock {
                                snapshots[provider.name] = snapshot
                            }
                            config.cache.save(provider.name, snapshot)
                            config.logger.info("FlagsManager", "Refreshed from ${provider.name}")
                            provider.name to snapshot
                        } catch (e: Exception) {
                            config.logger.error("FlagsManager", "Refresh failed for ${provider.name}", e)
                            null
                        }
                    }
            }
            
            results.awaitAll()
        }
        notifySnapshotUpdated("refresh")
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

    private fun notifySnapshotUpdated(source: String) {
        scope.launch {
            val listenersCopy = mutex.withLock { listeners.toList() }
            listenersCopy.forEach { it.onSnapshotUpdated(source) }
        }
    }

    private fun notifyOverrideChanged(key: FlagKey) {
        scope.launch {
            val listenersCopy = mutex.withLock { listeners.toList() }
            listenersCopy.forEach { it.onOverrideChanged(key) }
        }
    }
    
    /**
     * Update snapshot from realtime provider.
     * Internal method for RealtimeManager to update snapshots.
     */
    internal suspend fun updateSnapshotFromRealtime(providerName: String, snapshot: ProviderSnapshot) {
        mutex.withLock {
            snapshots[providerName] = snapshot
        }
        config.cache.save(providerName, snapshot)
        config.logger.info("FlagsManager", "Updated from realtime: $providerName")
        notifySnapshotUpdated("realtime")
    }
}

