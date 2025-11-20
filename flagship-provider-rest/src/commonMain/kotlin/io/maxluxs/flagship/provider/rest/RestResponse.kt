package io.maxluxs.flagship.provider.rest

import io.maxluxs.flagship.core.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
}

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

