package io.maxluxs.flagship.provider.firebase

import io.maxluxs.flagship.core.evaluator.BucketingEngine
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.SystemClock
import kotlinx.serialization.json.*

/**
 * Firebase Remote Config provider.
 * This is a placeholder implementation. Actual implementation requires Firebase SDK integration.
 */
class FirebaseRemoteConfigProvider(
    private val remoteConfigAdapter: FirebaseRemoteConfigAdapter,
    override val name: String = "firebase"
) : FlagsProvider {
    private var snapshot: ProviderSnapshot = ProviderSnapshot(
        flags = emptyMap(),
        experiments = emptyMap(),
        revision = null,
        fetchedAtMs = 0L
    )

    override suspend fun bootstrap(): ProviderSnapshot {
        remoteConfigAdapter.fetchAndActivate()
        snapshot = parseSnapshot()
        return snapshot
    }

    override suspend fun refresh(): ProviderSnapshot {
        remoteConfigAdapter.fetch()
        remoteConfigAdapter.activate()
        snapshot = parseSnapshot()
        return snapshot
    }

    override fun evaluateFlag(key: FlagKey, context: EvalContext): FlagValue? {
        return snapshot.flags[key]
    }

    override fun evaluateExperiment(key: ExperimentKey, context: EvalContext): ExperimentAssignment? {
        val experiment = snapshot.experiments[key] ?: return null
        return BucketingEngine.assign(experiment, context)
    }

    private fun parseSnapshot(): ProviderSnapshot {
        val allKeys = remoteConfigAdapter.getAllKeys()
        val flags = mutableMapOf<FlagKey, FlagValue>()
        val experiments = mutableMapOf<ExperimentKey, ExperimentDefinition>()

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

        return ProviderSnapshot(
            flags = flags,
            experiments = experiments,
            revision = null,
            fetchedAtMs = SystemClock.currentTimeMillis(),
            ttlMs = 15 * 60_000
        )
    }

    private fun parseFlag(value: String): FlagValue? {
        return when {
            value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true) -> {
                FlagValue.Bool(value.toBoolean())
            }
            value.toIntOrNull() != null -> {
                FlagValue.Int(value.toInt())
            }
            value.toDoubleOrNull() != null -> {
                FlagValue.Double(value.toDouble())
            }
            else -> {
                FlagValue.StringV(value)
            }
        }
    }

    private fun parseExperiment(key: String, value: String): ExperimentDefinition? {
        if (value.isBlank()) return null
        
        return try {
            val json = Json.parseToJsonElement(value).jsonObject
            
            // Parse variants
            val variantsJson = json["variants"]?.jsonArray ?: return null
            val variants = variantsJson.map { variantElement ->
                val variantObj = variantElement.jsonObject
                val name = variantObj["name"]?.jsonPrimitive?.content ?: "control"
                val weight = variantObj["weight"]?.jsonPrimitive?.double ?: 0.0
                val payloadObj = variantObj["payload"]?.jsonObject
                val payload = payloadObj?.entries?.associate { (key, value) -> 
                    key to (value.jsonPrimitive?.content ?: value.toString())
                } ?: emptyMap()
                Variant(name, weight, payload)
            }
            
            // Parse targeting (optional)
            val targeting = json["targeting"]?.jsonObject?.let { parseTargeting(it) }
            
            // Parse exposure type
            val exposureTypeStr = json["exposureType"]?.jsonPrimitive?.content ?: "onAssign"
            val exposureType = when (exposureTypeStr.lowercase()) {
                "onimpression" -> ExposureType.OnImpression
                else -> ExposureType.OnAssign
            }
            
            ExperimentDefinition(
                key = key,
                variants = variants,
                targeting = targeting,
                exposureType = exposureType
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseTargeting(json: JsonObject): TargetingRule? {
        val type = json["type"]?.jsonPrimitive?.content ?: return null
        
        return when (type) {
            "attribute_equals" -> {
                val key = json["key"]?.jsonPrimitive?.content ?: return null
                val value = json["value"]?.jsonPrimitive?.content ?: return null
                TargetingRule.AttributeEquals(key, value)
            }
            "region_in" -> {
                val regions = json["regions"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                    ?.toSet() ?: return null
                TargetingRule.RegionIn(regions)
            }
            "app_version_gte" -> {
                val version = json["version"]?.jsonPrimitive?.content ?: return null
                TargetingRule.AppVersionGte(version)
            }
            "composite" -> {
                val all = json["all"]?.jsonArray
                    ?.mapNotNull { parseTargeting(it.jsonObject) } ?: emptyList()
                val any = json["any"]?.jsonArray
                    ?.mapNotNull { parseTargeting(it.jsonObject) } ?: emptyList()
                TargetingRule.Composite(all, any)
            }
            else -> null
        }
    }
    
    private fun JsonObject.toMap(): Map<String, Any?> {
        return entries.associate { (key, value) ->
            key to when (value) {
                is JsonPrimitive -> when {
                    value.isString -> value.content
                    value.booleanOrNull != null -> value.boolean
                    value.intOrNull != null -> value.int
                    value.doubleOrNull != null -> value.double
                    else -> value.content
                }
                is JsonObject -> value.toMap()
                is JsonArray -> value.map {
                    when (it) {
                        is JsonPrimitive -> it.content
                        is JsonObject -> it.toMap()
                        else -> null
                    }
                }
                else -> null
            }
        }
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

