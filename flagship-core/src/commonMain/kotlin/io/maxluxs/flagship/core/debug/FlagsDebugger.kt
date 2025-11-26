package io.maxluxs.flagship.core.debug

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.provider.ProviderMetrics
import io.maxluxs.flagship.core.provider.ProviderMetricsTracker
import io.maxluxs.flagship.core.util.FlagsLogger

/**
 * Debugging tools for Flagship library.
 * 
 * Provides diagnostic information and verbose logging for troubleshooting.
 */
class FlagsDebugger(
    private val config: FlagsConfig,
    private val manager: FlagsManager,
    private val providers: List<FlagsProvider>,
    private val metricsTracker: ProviderMetricsTracker? = null,
    private val logger: FlagsLogger
) {
    /**
     * Get comprehensive debug information about the flags system.
     */
    suspend fun getDebugInfo(): DebugInfo {
        val providerInfos = providers.map { provider ->
            val metrics = metricsTracker?.getMetrics(provider.name)
            ProviderDebugInfo(
                name = provider.name,
                isHealthy = provider.isHealthy(),
                lastSuccessfulFetchMs = provider.getLastSuccessfulFetchMs(),
                consecutiveFailures = provider.getConsecutiveFailures(),
                metrics = metrics
            )
        }
        
        return DebugInfo(
            appKey = config.appKey,
            environment = config.environment,
            providers = providerInfos,
            cacheStats = getCacheStats(),
            configWarnings = getConfigWarnings()
        )
    }
    
    /**
     * Enable verbose logging mode.
     */
    fun enableVerboseLogging() {
        logger.info("FlagsDebugger", "Verbose logging enabled")
        // In a real implementation, this would configure the logger
    }
    
    /**
     * Get diagnostic information about a specific flag.
     */
    suspend fun diagnoseFlag(key: FlagKey, context: EvalContext?): FlagDiagnostics {
        val allFlags = manager.listAllFlags()
        val overrides = manager.listOverrides()
        
        val value = allFlags[key]
        val isOverridden = key in overrides
        val overrideValue = overrides[key]
        
        return FlagDiagnostics(
            key = key,
            value = value,
            isOverridden = isOverridden,
            overrideValue = overrideValue,
            availableInProviders = getProvidersWithFlag(key),
            context = context
        )
    }
    
    /**
     * Get diagnostic information about a specific experiment.
     */
    suspend fun diagnoseExperiment(key: ExperimentKey, context: EvalContext?): ExperimentDiagnostics {
        val assignment = context?.let { manager.assign(key, it) }
        
        return ExperimentDiagnostics(
            key = key,
            assignment = assignment,
            context = context,
            availableInProviders = getProvidersWithExperiment(key)
        )
    }
    
    private suspend fun getProvidersWithFlag(key: FlagKey): List<String> {
        return providers.filter { provider ->
            provider.isHealthy() && try {
                val context = EvalContext(
                    userId = "test", 
                    deviceId = "test",
                    appVersion = "1.0.0",
                    osName = "Unknown",
                    osVersion = "Unknown"
                )
                provider.evaluateFlag(key, context) != null
            } catch (e: Exception) {
                false
            }
        }.map { it.name }
    }
    
    private suspend fun getProvidersWithExperiment(key: ExperimentKey): List<String> {
        return providers.filter { provider ->
            provider.isHealthy() && try {
                val context = EvalContext(
                    userId = "test", 
                    deviceId = "test",
                    appVersion = "1.0.0",
                    osName = "Unknown",
                    osVersion = "Unknown"
                )
                provider.evaluateExperiment(key, context) != null
            } catch (e: Exception) {
                false
            }
        }.map { it.name }
    }
    
    private suspend fun getCacheStats(): Map<String, Any> {
        // In a real implementation, this would get stats from the cache
        return emptyMap()
    }
    
    private fun getConfigWarnings(): List<String> {
        val warnings = mutableListOf<String>()
        
        if (providers.isEmpty()) {
            warnings.add("No providers configured")
        }
        
        if (config.cache is io.maxluxs.flagship.core.cache.InMemoryCache) {
            warnings.add("Using InMemoryCache - data will be lost on restart")
        }
        
        return warnings
    }
}

/**
 * Debug information about the flags system.
 */
data class DebugInfo(
    val appKey: String,
    val environment: String,
    val providers: List<ProviderDebugInfo>,
    val cacheStats: Map<String, Any>,
    val configWarnings: List<String>
)

/**
 * Debug information about a provider.
 */
data class ProviderDebugInfo(
    val name: String,
    val isHealthy: Boolean,
    val lastSuccessfulFetchMs: Long?,
    val consecutiveFailures: Int,
    val metrics: ProviderMetrics?
)

/**
 * Diagnostic information about a flag.
 */
data class FlagDiagnostics(
    val key: FlagKey,
    val value: FlagValue?,
    val isOverridden: Boolean,
    val overrideValue: FlagValue?,
    val availableInProviders: List<String>,
    val context: EvalContext?
)

/**
 * Diagnostic information about an experiment.
 */
data class ExperimentDiagnostics(
    val key: ExperimentKey,
    val assignment: ExperimentAssignment?,
    val context: EvalContext?,
    val availableInProviders: List<String>
)

