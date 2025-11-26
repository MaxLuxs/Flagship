package io.maxluxs.flagship.shared.api

import io.maxluxs.flagship.core.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Shared REST API models used across multiple modules.
 * 
 * These models are used for:
 * - REST provider (flagship-provider-rest)
 * - Admin UI (flagship-admin-ui-compose)
 * - Server API (flagship-server)
 */

/**
 * REST representation of a flag value.
 */
@Serializable
data class RestFlagValue(
    val type: String,
    val value: JsonElement
) {
    fun toFlagValue(): FlagValue {
        return when (type) {
            "bool" -> FlagValue.Bool(value.toString().toBoolean())
            "int" -> FlagValue.Int(value.toString().toInt())
            "double" -> FlagValue.Double(value.toString().toDouble())
            "string" -> FlagValue.StringV(value.toString().trim('"'))
            "json" -> FlagValue.Json(value)
            else -> FlagValue.StringV(value.toString())
        }
    }
    
    companion object {
        fun fromFlagValue(flagValue: FlagValue): RestFlagValue {
            return when (flagValue) {
                is FlagValue.Bool -> RestFlagValue("bool", kotlinx.serialization.json.JsonPrimitive(flagValue.value))
                is FlagValue.Int -> RestFlagValue("int", kotlinx.serialization.json.JsonPrimitive(flagValue.value))
                is FlagValue.Double -> RestFlagValue("double", kotlinx.serialization.json.JsonPrimitive(flagValue.value))
                is FlagValue.StringV -> RestFlagValue("string", kotlinx.serialization.json.JsonPrimitive(flagValue.value))
                is FlagValue.Json -> RestFlagValue("json", flagValue.value)
                is FlagValue.Date -> RestFlagValue("date", kotlinx.serialization.json.JsonPrimitive(flagValue.value))
                is FlagValue.Enum -> RestFlagValue("enum", kotlinx.serialization.json.JsonPrimitive(flagValue.value))
                is FlagValue.List -> RestFlagValue("list", kotlinx.serialization.json.JsonArray(flagValue.value.map { 
                    kotlinx.serialization.json.JsonPrimitive(it.toString())
                }))
                is FlagValue.Map -> RestFlagValue("map", kotlinx.serialization.json.JsonObject(flagValue.value.mapValues { 
                    kotlinx.serialization.json.JsonPrimitive(it.value.toString())
                }))
            }
        }
    }
}

/**
 * REST representation of an experiment.
 */
@Serializable
data class RestExperiment(
    val variants: List<RestVariant>,
    val targeting: RestTargeting? = null,
    val exposureType: String = "onAssign"
) {
    fun toExperimentDefinition(key: String): ExperimentDefinition {
        return ExperimentDefinition(
            key = key,
            variants = variants.map { it.toVariant() },
            targeting = targeting?.toTargetingRule(),
            exposureType = when (exposureType.lowercase()) {
                "onimpression" -> ExposureType.OnImpression
                else -> ExposureType.OnAssign
            }
        )
    }
}

/**
 * REST representation of an experiment variant.
 */
@Serializable
data class RestVariant(
    val name: String,
    val weight: Double,
    val payload: Map<String, String> = emptyMap()
) {
    fun toVariant(): Variant {
        return Variant(
            name = name,
            weight = weight,
            payload = payload
        )
    }
}

/**
 * REST representation of targeting rules.
 */
@Serializable
data class RestTargeting(
    val type: String,
    val key: String? = null,
    val value: String? = null,
    val regions: List<String>? = null,
    val version: String? = null,
    val all: List<RestTargeting>? = null,
    val any: List<RestTargeting>? = null
) {
    fun toTargetingRule(): TargetingRule? {
        return when (type) {
            "attribute_equals" -> {
                if (key != null && value != null) {
                    TargetingRule.AttributeEquals(key, value)
                } else null
            }
            "region_in" -> {
                regions?.let { TargetingRule.RegionIn(it.toSet()) }
            }
            "app_version_gte" -> {
                version?.let { TargetingRule.AppVersionGte(it) }
            }
            "composite" -> {
                TargetingRule.Composite(
                    all = all?.mapNotNull { it.toTargetingRule() } ?: emptyList(),
                    any = any?.mapNotNull { it.toTargetingRule() } ?: emptyList()
                )
            }
            else -> null
        }
    }
}

/**
 * Complete REST response containing flags and experiments.
 */
@Serializable
data class RestResponse(
    val revision: String? = null,
    val fetchedAt: Long,
    val ttlMs: Long? = null,
    val flags: Map<String, RestFlagValue> = emptyMap(),
    val experiments: Map<String, RestExperiment> = emptyMap()
) {
    fun toProviderSnapshot(): ProviderSnapshot {
        return ProviderSnapshot(
            flags = flags.mapValues { it.value.toFlagValue() },
            experiments = experiments.mapValues { it.value.toExperimentDefinition(it.key) },
            revision = revision,
            fetchedAtMs = fetchedAt,
            ttlMs = ttlMs
        )
    }
}

