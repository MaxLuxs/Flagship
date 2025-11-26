package io.maxluxs.flagship.core.provider.env

import io.maxluxs.flagship.core.errors.ProviderException
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.BaseFlagsProvider
import io.maxluxs.flagship.core.util.Clock
import io.maxluxs.flagship.core.util.FlagValueParser
import io.maxluxs.flagship.core.util.FlagsLogger
import io.maxluxs.flagship.core.util.NoopLogger
import io.maxluxs.flagship.core.util.SystemClock
import io.maxluxs.flagship.core.util.currentTimeMs
import io.maxluxs.flagship.core.util.getEnvironmentVariables

/**
 * Environment variables provider for reading flags from system environment variables.
 * 
 * Useful for:
 * - Docker/Kubernetes deployments
 * - CI/CD pipelines
 * - Local development with .env files
 * - Configuration via environment
 * 
 * Environment variable format:
 * - `FLAG_<key>=<value>` for flags
 * - `EXP_<key>=<json>` for experiments
 * 
 * Example:
 * ```
 * FLAG_NEW_FEATURE=true
 * FLAG_MAX_RETRIES=5
 * FLAG_API_URL=https://api.example.com
 * ```
 * 
 * @property prefix Prefix for environment variables (default: "FLAG_")
 * @property experimentPrefix Prefix for experiment variables (default: "EXP_")
 * @property name Provider name (default: "env")
 * @property getEnv Function to get environment variable (default: System.getenv)
 * @property clock Clock for time tracking
 * @property logger Logger for debug messages
 */
class EnvFlagsProvider(
    private val prefix: String = "FLAG_",
    private val experimentPrefix: String = "EXP_",
    name: String = "env",
    private val getEnv: (String) -> String? = { getEnvironmentVariables()[it] },
    private val clock: Clock = SystemClock,
    private val logger: FlagsLogger = NoopLogger
) : BaseFlagsProvider(name, clock) {
    
    override suspend fun fetchSnapshot(currentRevision: String?): ProviderSnapshot {
        return loadFromEnvironment()
    }
    
    override suspend fun refresh(): ProviderSnapshot {
        // Environment variables don't change during runtime, so return cached snapshot
        return snapshot
    }
    
    private suspend fun loadFromEnvironment(): ProviderSnapshot {
        val flags = mutableMapOf<FlagKey, FlagValue>()
        val experiments = mutableMapOf<ExperimentKey, ExperimentDefinition>()
        
        // Get all environment variables
        val allEnvVars = getAllEnvironmentVariables()
        
        allEnvVars.forEach { (key, value) ->
            when {
                key.startsWith(experimentPrefix) -> {
                    // Parse experiment
                    val experimentKey = key.removePrefix(experimentPrefix).lowercase()
                    parseExperiment(experimentKey, value)?.let {
                        experiments[experimentKey] = it
                    }
                }
                key.startsWith(prefix) -> {
                    // Parse flag
                    val flagKey = key.removePrefix(prefix).lowercase()
                    parseFlag(flagKey, value)?.let {
                        flags[flagKey] = it
                    }
                }
            }
        }
        
        logger.info(name, "Loaded ${flags.size} flags and ${experiments.size} experiments from environment")
        
        return ProviderSnapshot(
            flags = flags,
            experiments = experiments,
            revision = null,
            fetchedAtMs = clock.currentTimeMs(),
            ttlMs = null // Environment variables don't expire
        )
    }
    
    private fun getAllEnvironmentVariables(): Map<String, String> {
        // Use platform-specific function to get environment variables
        val envVars = mutableMapOf<String, String>()
        
        try {
            val processEnv = getEnvironmentVariables()
            processEnv.forEach { (key, value) ->
                if (key.startsWith(prefix) || key.startsWith(experimentPrefix)) {
                    envVars[key] = value
                }
            }
        } catch (e: Exception) {
            logger.warn(name, "Failed to read environment variables: ${e.message}")
        }
        
        return envVars
    }
    
    private fun parseFlag(key: String, value: String): FlagValue? {
        return try {
            FlagValueParser.parseFromString(value)
        } catch (e: Exception) {
            logger.warn(name, "Failed to parse flag $key: ${e.message}")
            null
        }
    }
    
    private fun parseExperiment(key: String, value: String): ExperimentDefinition? {
        // Experiments from env vars should be JSON strings
        return try {
            // Simple parsing - in production, use proper JSON parser
            // For now, return null as experiments require complex structure
            logger.warn(name, "Experiment parsing from env vars not fully implemented")
            null
        } catch (e: Exception) {
            logger.warn(name, "Failed to parse experiment $key: ${e.message}")
            null
        }
    }
}

