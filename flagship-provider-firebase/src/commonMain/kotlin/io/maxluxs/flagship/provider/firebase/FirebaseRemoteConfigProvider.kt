package io.maxluxs.flagship.provider.firebase

import io.maxluxs.flagship.core.errors.ProviderException
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.BaseFlagsProvider
import io.maxluxs.flagship.core.provider.ProviderErrorHandler
import io.maxluxs.flagship.core.retry.ExponentialBackoffRetry
import io.maxluxs.flagship.core.retry.RetryPolicy
import io.maxluxs.flagship.core.util.ExperimentParser
import io.maxluxs.flagship.core.util.FlagValueParser
import io.maxluxs.flagship.core.util.FlagsLogger
import io.maxluxs.flagship.core.util.NoopLogger
import io.maxluxs.flagship.core.util.SystemClock
import kotlinx.serialization.json.*

/**
 * Firebase Remote Config provider with error handling, retry logic, and caching.
 * 
 * Features:
 * - Retry logic with exponential backoff
 * - Error handling for Firebase-specific errors
 * - Caching of last successful snapshot
 * - Support for remote config templates
 * 
 * @property remoteConfigAdapter Firebase Remote Config adapter
 * @property name Provider name (default: "firebase")
 * @property retryPolicy Retry policy (default: ExponentialBackoffRetry with 3 attempts)
 * @property logger Logger for debug messages
 */
class FirebaseRemoteConfigProvider(
    private val remoteConfigAdapter: FirebaseRemoteConfigAdapter,
    name: String = "firebase",
    private val retryPolicy: RetryPolicy = ExponentialBackoffRetry(maxAttempts = 3),
    private val logger: FlagsLogger = NoopLogger
) : BaseFlagsProvider(name) {
    
    init {
        // Initialize error handler for common error handling
        errorHandler = ProviderErrorHandler(
            providerName = name,
            retryPolicy = retryPolicy,
            logger = logger,
            snapshotCache = snapshotCache
        )
    }
    
    override suspend fun fetchSnapshot(currentRevision: String?): ProviderSnapshot {
        return errorHandler?.fetchWithFallback {
            fetchWithErrorHandling()
        } ?: fetchWithErrorHandling() // Fallback if errorHandler not set
    }
    
    override suspend fun refresh(): ProviderSnapshot {
        return errorHandler?.fetchWithFallback {
            fetchWithErrorHandling()
        } ?: fetchWithErrorHandling() // Fallback if errorHandler not set
    }
    
    private suspend fun fetchWithErrorHandling(): ProviderSnapshot {
        return try {
            remoteConfigAdapter.fetchAndActivate()
            val snapshot = parseSnapshot()
            
            // Cache successful snapshot (also cached in BaseFlagsProvider.bootstrap/refresh)
            snapshotCache.update(snapshot)
            
            logger.info(name, "Successfully fetched snapshot from Firebase")
            snapshot
        } catch (e: Exception) {
            // Use errorHandler to categorize error if available
            throw errorHandler?.categorizeError(e, "Firebase") 
                ?: ProviderException(name, "Firebase error: ${e.message}", e)
        }
    }

    private fun parseSnapshot(): ProviderSnapshot {
        val allKeys = remoteConfigAdapter.getAllKeys()
        val flags = mutableMapOf<FlagKey, FlagValue>()
        val experiments = mutableMapOf<ExperimentKey, ExperimentDefinition>()

        // Check for template-based structure (flagship_flags, flagship_experiments)
        val hasTemplate = allKeys.contains("flagship_flags") || allKeys.contains("flagship_experiments")
        
        if (hasTemplate) {
            // Parse template-based structure
            parseTemplateStructure(flags, experiments)
        } else {
            // Parse flat structure (legacy)
            allKeys.forEach { key ->
                when {
                    key.startsWith("exp_") -> {
                        // Parse experiment
                        val value = remoteConfigAdapter.getString(key)
                        parseExperiment(key, value)?.let { experiments[key] = it }
                    }

                    else -> {
                        // Parse flag
                        val value = remoteConfigAdapter.getString(key)
                        parseFlag(value)?.let { flags[key] = it }
                    }
                }
            }
        }

        return ProviderSnapshot(
            flags = flags,
            experiments = experiments,
            revision = null,
            fetchedAtMs = SystemClock.currentTimeMillis(),
            ttlMs = 15 * 60_000
        )
    }
    
    /**
     * Parse template-based structure from Firebase Remote Config.
     * 
     * Supports two template formats:
     * 1. JSON string in "flagship_flags" and "flagship_experiments" keys
     * 2. Flat structure with "flagship_flags.{key}" and "flagship_experiments.{key}" keys
     */
    private fun parseTemplateStructure(
        flags: MutableMap<FlagKey, FlagValue>,
        experiments: MutableMap<ExperimentKey, ExperimentDefinition>
    ) {
        // Try to parse JSON template first
        val flagsJson = remoteConfigAdapter.getString("flagship_flags")
        val experimentsJson = remoteConfigAdapter.getString("flagship_experiments")
        
        if (flagsJson.isNotBlank()) {
            try {
                val parsedFlags = parseJsonTemplate(flagsJson)
                flags.putAll(parsedFlags)
            } catch (e: Exception) {
                logger.warn(name, "Failed to parse flagship_flags JSON template: ${e.message}")
                // Fall back to flat structure parsing
                parseFlatTemplateStructure("flagship_flags", flags, experiments)
            }
        }
        
        if (experimentsJson.isNotBlank()) {
            try {
                val parsedExperiments = parseExperimentsJsonTemplate(experimentsJson)
                experiments.putAll(parsedExperiments)
            } catch (e: Exception) {
                logger.warn(name, "Failed to parse flagship_experiments JSON template: ${e.message}")
                // Fall back to flat structure parsing
                parseFlatTemplateStructure("flagship_experiments", flags, experiments)
            }
        }
        
        // Also check for flat template structure (flagship_flags.{key})
        if (flags.isEmpty() && experiments.isEmpty()) {
            parseFlatTemplateStructure("flagship_flags", flags, experiments)
            parseFlatTemplateStructure("flagship_experiments", flags, experiments)
        }
    }
    
    /**
     * Parse JSON template for flags.
     * 
     * Expected format:
     * ```json
     * {
     *   "flag_key": true,
     *   "another_flag": "value",
     *   "numeric_flag": 42
     * }
     * ```
     */
    private fun parseJsonTemplate(json: String): Map<FlagKey, FlagValue> {
        val result = mutableMapOf<FlagKey, FlagValue>()
        
        try {
            val jsonElement = kotlinx.serialization.json.Json.parseToJsonElement(json)
            val jsonObject = jsonElement.jsonObject
            
            jsonObject.forEach { (key, value) ->
                val flagValue = when (value) {
                    is JsonPrimitive -> {
                        when {
                            value.isString -> FlagValue.StringV(value.content)
                            value.booleanOrNull != null -> FlagValue.Bool(value.boolean)
                            value.intOrNull != null -> FlagValue.Int(value.int)
                            value.doubleOrNull != null -> FlagValue.Double(value.double)
                            else -> null
                        }
                    }
                    is JsonObject, is JsonArray -> {
                        FlagValue.Json(value)
                    }
                    else -> null
                }
                
                flagValue?.let { result[key] = it }
            }
        } catch (e: Exception) {
            logger.warn(name, "Failed to parse JSON template: ${e.message}")
        }
        
        return result
    }
    
    /**
     * Parse JSON template for experiments.
     * 
     * Expected format:
     * ```json
     * {
     *   "experiment_key": {
     *     "variants": [
     *       { "name": "control", "weight": 0.5 },
     *       { "name": "variant_a", "weight": 0.5 }
     *     ],
     *     "targeting": { ... }
     *   }
     * }
     * ```
     */
    private fun parseExperimentsJsonTemplate(json: String): Map<ExperimentKey, ExperimentDefinition> {
        val result = mutableMapOf<ExperimentKey, ExperimentDefinition>()
        
        try {
            val jsonElement = kotlinx.serialization.json.Json.parseToJsonElement(json)
            val jsonObject = jsonElement.jsonObject
            
            jsonObject.forEach { (key, value) ->
                if (value is JsonObject) {
                    val experiment = ExperimentParser.parseExperimentFromJson(key, value)
                    experiment?.let { result[key] = it }
                }
            }
        } catch (e: Exception) {
            logger.warn(name, "Failed to parse experiments JSON template: ${e.message}")
        }
        
        return result
    }
    
    /**
     * Parse flat template structure (flagship_flags.{key}).
     */
    private fun parseFlatTemplateStructure(
        prefix: String,
        flags: MutableMap<FlagKey, FlagValue>,
        experiments: MutableMap<ExperimentKey, ExperimentDefinition>
    ) {
        val allKeys = remoteConfigAdapter.getAllKeys()
        allKeys.forEach { key ->
            when {
                key.startsWith("$prefix.") -> {
                    val actualKey = key.removePrefix("$prefix.")
                    val value = remoteConfigAdapter.getString(key)
                    
                    if (prefix == "flagship_flags") {
                        parseFlag(value)?.let { flags[actualKey] = it }
                    } else if (prefix == "flagship_experiments") {
                        parseExperiment(actualKey, value)?.let { experiments[actualKey] = it }
                    }
                }
            }
        }
    }

    private fun parseFlag(value: String): FlagValue? {
        return FlagValueParser.parseFromString(value)
    }

    private fun parseExperiment(key: String, value: String): ExperimentDefinition? {
        return ExperimentParser.parseExperiment(key, value)
    }
}

/**
 * Adapter interface for Firebase Remote Config.
 * Actual implementation should wrap Firebase SDK.
 */
interface FirebaseRemoteConfigAdapter {
    suspend fun fetchAndActivate()
    suspend fun fetch()
    suspend fun activate()
    fun getString(key: String): String
    fun getAllKeys(): Set<String>
}
