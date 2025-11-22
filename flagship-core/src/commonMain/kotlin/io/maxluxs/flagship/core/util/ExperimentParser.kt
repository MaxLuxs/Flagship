package io.maxluxs.flagship.core.util

import io.maxluxs.flagship.core.model.ExperimentDefinition
import io.maxluxs.flagship.core.model.ExposureType
import io.maxluxs.flagship.core.model.TargetingRule
import io.maxluxs.flagship.core.model.Variant
import kotlinx.serialization.json.*

/**
 * Utility for parsing experiment definitions from JSON.
 * 
 * Used by multiple providers (Firebase, LaunchDarkly) to avoid code duplication.
 */
object ExperimentParser {
    /**
     * Parse experiment definition from JSON string.
     * 
     * @param key Experiment key
     * @param jsonString JSON string containing experiment definition
     * @return ExperimentDefinition or null if parsing fails
     */
    fun parseExperiment(key: String, jsonString: String): ExperimentDefinition? {
        if (jsonString.isBlank()) return null
        
        return try {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            parseExperimentFromJson(key, json)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse experiment definition from JsonObject.
     * 
     * @param key Experiment key
     * @param json JsonObject containing experiment definition
     * @return ExperimentDefinition or null if parsing fails
     */
    fun parseExperimentFromJson(key: String, json: JsonObject): ExperimentDefinition? {
        return try {
            // Parse variants
            val variantsJson = json["variants"]?.jsonArray ?: return null
            val variants = variantsJson.mapNotNull { variantElement ->
                parseVariant(variantElement.jsonObject)
            }
            
            if (variants.isEmpty()) return null
            
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
    
    private fun parseVariant(variantObj: JsonObject): Variant? {
        val name = variantObj["name"]?.jsonPrimitive?.content ?: "control"
        val weight = variantObj["weight"]?.jsonPrimitive?.double ?: 0.0
        val payloadObj = variantObj["payload"]?.jsonObject
        val payload = payloadObj?.entries?.associate { (k, v) ->
            k to (v.jsonPrimitive?.content ?: v.toString())
        } ?: emptyMap()
        
        return Variant(name, weight, payload)
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

