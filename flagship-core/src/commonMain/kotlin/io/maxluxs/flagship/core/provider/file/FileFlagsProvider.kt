package io.maxluxs.flagship.core.provider.file

import io.maxluxs.flagship.core.errors.ParseException
import io.maxluxs.flagship.core.errors.ProviderException
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.BaseFlagsProvider
import io.maxluxs.flagship.core.util.Clock
import io.maxluxs.flagship.core.util.FlagsLogger
import io.maxluxs.flagship.core.util.NoopLogger
import io.maxluxs.flagship.core.util.SystemClock
import io.maxluxs.flagship.core.util.currentTimeMs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * File-based provider for reading flags from local JSON/YAML files.
 * 
 * Useful for:
 * - Development and testing
 * - Offline scenarios
 * - CI/CD environments
 * - Hot reload during development
 * 
 * @property filePath Path to the JSON file containing flags
 * @property name Provider name (default: "file")
 * @property hotReload If true, re-reads file on each refresh (default: false)
 * @property clock Clock for time tracking
 * @property logger Logger for debug messages
 */
class FileFlagsProvider(
    private val filePath: String,
    name: String = "file",
    private val hotReload: Boolean = false,
    private val clock: Clock = SystemClock,
    private val logger: FlagsLogger = NoopLogger
) : BaseFlagsProvider(name, clock) {
    
    private var lastModified: Long = 0
    
    override suspend fun fetchSnapshot(currentRevision: String?): ProviderSnapshot {
        return loadFromFile()
    }
    
    override suspend fun refresh(): ProviderSnapshot {
        if (hotReload || hasFileChanged()) {
            return loadFromFile()
        }
        return snapshot
    }
    
    private suspend fun hasFileChanged(): Boolean {
        // Platform-specific implementation needed
        // For now, always return true to allow hot reload
        return hotReload
    }
    
    private suspend fun loadFromFile(): ProviderSnapshot {
        return try {
            // Platform-specific file reading
            val content = readFileContentInternal(filePath)
            
            if (content == null) {
                throw ProviderException(
                    name,
                    "File not found or cannot be read: $filePath"
                )
            }
            
            // Parse JSON
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
            val jsonObject = try {
                json.parseToJsonElement(content).jsonObject
            } catch (e: Exception) {
                throw ParseException("Failed to parse JSON file: ${e.message}", e)
            }
            
            val snapshot = parseJsonToSnapshot(jsonObject, json)
            lastModified = clock.currentTimeMs()
            
            logger.info(name, "Successfully loaded snapshot from file: $filePath")
            snapshot
        } catch (e: ProviderException) {
            throw e
        } catch (e: ParseException) {
            throw e
        } catch (e: Exception) {
            throw ProviderException(
                name,
                "Failed to load file: ${e.message}",
                e
            )
        }
    }
    
    /**
     * Platform-specific function to read file content.
     * Delegates to platform-specific implementation.
     */
    protected suspend fun readFileContentInternal(filePath: String): String? {
        return readFileContent(this, filePath)
    }
    
    /**
     * Parse JSON object to ProviderSnapshot.
     * Local implementation to avoid dependency on flagship-shared.
     */
    private fun parseJsonToSnapshot(jsonObject: JsonObject, json: Json): ProviderSnapshot {
        val revision = jsonObject["revision"]?.let { it.toString().trim('"') }
        val fetchedAt = jsonObject["fetchedAt"]?.let { 
            if (it is kotlinx.serialization.json.JsonPrimitive) it.content.toLongOrNull() else null
        } ?: clock.currentTimeMs()
        val ttlMs = jsonObject["ttlMs"]?.let { 
            if (it is kotlinx.serialization.json.JsonPrimitive) it.content.toLongOrNull() else null
        }
        
        val flags = mutableMapOf<FlagKey, FlagValue>()
        val experiments = mutableMapOf<ExperimentKey, ExperimentDefinition>()
        
        // Parse flags
        jsonObject["flags"]?.jsonObject?.forEach { (key, value) ->
            try {
                val flagValue = parseRestFlagValue(value.jsonObject, json)
                flags[key] = flagValue
            } catch (e: Exception) {
                logger.warn(name, "Failed to parse flag $key: ${e.message}")
            }
        }
        
        // Parse experiments
        jsonObject["experiments"]?.jsonObject?.forEach { (key, value) ->
            try {
                val experiment = parseRestExperiment(value.jsonObject, key, json)
                experiments[key] = experiment
            } catch (e: Exception) {
                logger.warn(name, "Failed to parse experiment $key: ${e.message}")
            }
        }
        
        return ProviderSnapshot(
            flags = flags,
            experiments = experiments,
            revision = revision,
            fetchedAtMs = fetchedAt,
            ttlMs = ttlMs
        )
    }
    
    private fun parseRestFlagValue(flagObj: JsonObject, json: Json): FlagValue {
        val type = flagObj["type"]?.let { 
            if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
        } ?: "string"
        val valueElement = flagObj["value"] ?: throw ParseException("Missing value in flag")
        
        return when (type) {
            "bool" -> FlagValue.Bool(
                if (valueElement is kotlinx.serialization.json.JsonPrimitive) valueElement.content.toBoolean() else false
            )
            "int" -> FlagValue.Int(
                if (valueElement is kotlinx.serialization.json.JsonPrimitive) valueElement.content.toIntOrNull() ?: 0 else 0
            )
            "double" -> FlagValue.Double(
                if (valueElement is kotlinx.serialization.json.JsonPrimitive) valueElement.content.toDoubleOrNull() ?: 0.0 else 0.0
            )
            "string" -> FlagValue.StringV(
                if (valueElement is kotlinx.serialization.json.JsonPrimitive) valueElement.content.trim('"') else valueElement.toString()
            )
            "json" -> FlagValue.Json(valueElement)
            else -> FlagValue.StringV(valueElement.toString())
        }
    }
    
    private fun parseRestExperiment(expObj: JsonObject, key: String, json: Json): ExperimentDefinition {
        val variantsJson = expObj["variants"]?.let { 
            if (it is kotlinx.serialization.json.JsonArray) {
                it.mapNotNull { elem -> elem as? JsonObject }
            } else emptyList()
        } ?: emptyList()
        val variants = variantsJson.map { variantObj ->
            val name = variantObj["name"]?.let { 
                if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
            } ?: ""
            val weight = variantObj["weight"]?.let { 
                if (it is kotlinx.serialization.json.JsonPrimitive) it.content.toDoubleOrNull() else null
            } ?: 0.0
            val payload = variantObj["payload"]?.let { 
                if (it is JsonObject) {
                    it.entries.associate { (k, v) -> 
                        k to (if (v is kotlinx.serialization.json.JsonPrimitive) v.content else v.toString())
                    }
                } else emptyMap()
            } ?: emptyMap()
            Variant(name, weight, payload)
        }
        
        val targeting = expObj["targeting"]?.jsonObject?.let { parseRestTargeting(it, json) }
        val exposureTypeStr = expObj["exposureType"]?.let { 
            if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
        } ?: "onAssign"
        val exposureType = when (exposureTypeStr.lowercase()) {
            "onimpression" -> ExposureType.OnImpression
            else -> ExposureType.OnAssign
        }
        
        return ExperimentDefinition(
            key = key,
            variants = variants,
            targeting = targeting,
            exposureType = exposureType
        )
    }
    
    private fun parseRestTargeting(targetingObj: JsonObject, json: Json): TargetingRule? {
        val type = targetingObj["type"]?.let { 
            if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
        } ?: return null
        
        return when (type) {
            "attribute_equals" -> {
                val key = targetingObj["key"]?.let { 
                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
                }
                val value = targetingObj["value"]?.let { 
                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
                }
                if (key != null && value != null) {
                    TargetingRule.AttributeEquals(key, value)
                } else null
            }
            "region_in" -> {
                val regions = targetingObj["regions"]?.let { 
                    if (it is kotlinx.serialization.json.JsonArray) {
                        it.mapNotNull { elem -> 
                            if (elem is kotlinx.serialization.json.JsonPrimitive) elem.content else null
                        }
                    } else null
                }
                regions?.let { TargetingRule.RegionIn(it.toSet()) }
            }
            "app_version_gte" -> {
                val version = targetingObj["version"]?.let { 
                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content else null
                }
                version?.let { TargetingRule.AppVersionGte(it) }
            }
            "composite" -> {
                val all = targetingObj["all"]?.let { 
                    if (it is kotlinx.serialization.json.JsonArray) {
                        it.mapNotNull { elem -> 
                            (elem as? JsonObject)?.let { parseRestTargeting(it, json) }
                        }
                    } else emptyList()
                } ?: emptyList()
                val any = targetingObj["any"]?.let { 
                    if (it is kotlinx.serialization.json.JsonArray) {
                        it.mapNotNull { elem -> 
                            (elem as? JsonObject)?.let { parseRestTargeting(it, json) }
                        }
                    } else emptyList()
                } ?: emptyList()
                TargetingRule.Composite(all, any)
            }
            else -> null
        }
    }
}

/**
 * Platform-specific function to read file content.
 */
internal expect suspend fun readFileContent(provider: FileFlagsProvider, filePath: String): String?

