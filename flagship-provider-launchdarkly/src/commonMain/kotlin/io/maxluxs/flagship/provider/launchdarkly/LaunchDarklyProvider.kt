package io.maxluxs.flagship.provider.launchdarkly

import io.maxluxs.flagship.core.evaluator.BucketingEngine
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.SystemClock
import kotlinx.serialization.json.*

/**
 * LaunchDarkly provider for Flagship.
 * Integrates with LaunchDarkly SDK for enterprise feature flag management.
 */
class LaunchDarklyProvider(
    private val adapter: LaunchDarklyAdapter,
    override val name: String = "launchdarkly"
) : FlagsProvider {
    
    private var snapshot: ProviderSnapshot = ProviderSnapshot(
        flags = emptyMap(),
        experiments = emptyMap(),
        revision = null,
        fetchedAtMs = 0L
    )

    override suspend fun bootstrap(): ProviderSnapshot {
        adapter.initialize()
        snapshot = parseSnapshot()
        return snapshot
    }

    override suspend fun refresh(): ProviderSnapshot {
        adapter.refresh()
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
        val allFlags = adapter.getAllFlags()
        val flags = mutableMapOf<FlagKey, FlagValue>()
        val experiments = mutableMapOf<ExperimentKey, ExperimentDefinition>()

        allFlags.forEach { (key, value) ->
            when {
                key.startsWith("exp_") -> {
                    // Parse as experiment
                    parseExperiment(key, value)?.let { experiments[key] = it }
                }
                else -> {
                    // Parse as simple flag
                    parseFlag(value)?.let { flags[key] = it }
                }
            }
        }

        return ProviderSnapshot(
            flags = flags,
            experiments = experiments,
            revision = adapter.getRevision(),
            fetchedAtMs = SystemClock.currentTimeMillis(),
            ttlMs = 15 * 60_000 // 15 minutes default
        )
    }

    private fun parseFlag(value: Any?): FlagValue? {
        return when (value) {
            is Boolean -> FlagValue.Bool(value)
            is Int -> FlagValue.Int(value)
            is Long -> FlagValue.Int(value.toInt())
            is Double -> FlagValue.Double(value)
            is Float -> FlagValue.Double(value.toDouble())
            is String -> FlagValue.StringV(value)
            else -> null
        }
    }

    private fun parseExperiment(key: String, value: Any?): ExperimentDefinition? {
        if (value !is String) return null
        
        return try {
            val json = Json.parseToJsonElement(value).jsonObject
            
            // Parse variants
            val variantsJson = json["variants"]?.jsonArray ?: return null
            val variants = variantsJson.map { variantElement ->
                val variantObj = variantElement.jsonObject
                val name = variantObj["name"]?.jsonPrimitive?.content ?: "control"
                val weight = variantObj["weight"]?.jsonPrimitive?.double ?: 0.0
                val payloadObj = variantObj["payload"]?.jsonObject
                val payload = payloadObj?.entries?.associate { (k, v) -> 
                    k to (v.jsonPrimitive?.content ?: v.toString())
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
}

/**
 * Adapter interface for LaunchDarkly SDK.
 * Platform-specific implementations wrap native LaunchDarkly SDKs.
 */
interface LaunchDarklyAdapter {
    suspend fun initialize()
    suspend fun refresh()
    fun getAllFlags(): Map<String, Any?>
    fun getRevision(): String?
}

