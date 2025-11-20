package io.maxluxs.flagship.core.manager

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.evaluator.FlagsEvaluator
import io.maxluxs.flagship.core.model.*
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
    private val exposureTracker = io.maxluxs.flagship.core.analytics.ExposureTracker()
    
    private var defaultContext: EvalContext? = null
    private var bootstrapped = false
    private val bootstrapMutex = Mutex()

    fun setDefaultContext(context: EvalContext) {
        defaultContext = context
    }

    override fun isEnabled(key: FlagKey, default: Boolean, ctx: EvalContext?): Boolean {
        val context = ctx ?: defaultContext ?: return default
        val value = evaluateInternal(key, FlagValue.Bool(default))
        return value?.asBoolean() ?: default
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> value(key: FlagKey, default: T, ctx: EvalContext?): T {
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

    override fun assign(key: ExperimentKey, ctx: EvalContext?): ExperimentAssignment? {
        val context = ctx ?: defaultContext ?: return null
        // Snapshots are read-only list copies in evaluateExperiment, so no lock needed there if passed correctly
        // But we need lock to get snapshots values safely
        val currentSnapshots = runBlocking { 
            mutex.withLock { snapshots.values.toList() } 
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

    override fun listOverrides(): Map<FlagKey, FlagValue> = runBlocking {
        mutex.withLock { overrides.toMap() }
    }
    
    override fun listAllFlags(): Map<FlagKey, FlagValue> = runBlocking {
        mutex.withLock {
            val allFlags = mutableMapOf<FlagKey, FlagValue>()
            snapshots.values.forEach { snapshot ->
                allFlags.putAll(snapshot.flags)
            }
            allFlags.toMap()
        }
    }

    private fun evaluateInternal(key: FlagKey, default: FlagValue?): FlagValue? {
        val (currentOverrides, currentSnapshots) = runBlocking {
            mutex.withLock {
                overrides.toMap() to snapshots.values.toList()
            }
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
        }
    }

    private suspend fun refreshInternal() {
        config.logger.info("FlagsManager", "Starting refresh...")
        
        coroutineScope {
            val results = config.providers.map { provider ->
                async {
                    try {
                        val snapshot = provider.refresh()
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
}

