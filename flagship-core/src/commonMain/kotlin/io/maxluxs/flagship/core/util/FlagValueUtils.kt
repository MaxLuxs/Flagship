package io.maxluxs.flagship.core.util

import io.maxluxs.flagship.core.model.FlagKey
import io.maxluxs.flagship.core.model.FlagValue

/**
 * Utilities for working with FlagValue in REST APIs and serialization.
 */
object FlagValueUtils {
    /**
     * Convert FlagValue to JSON-serializable value.
     * 
     * Used by REST controllers to serialize flag values.
     * 
     * @param value The flag value to convert
     * @return JSON-serializable value (Boolean, Int, Double, String, or String representation)
     */
    fun toJsonValue(value: FlagValue): Any? {
        return when (value) {
            is FlagValue.Bool -> value.value
            is FlagValue.Int -> value.value
            is FlagValue.Double -> value.value
            is FlagValue.StringV -> value.value
            is FlagValue.Json -> value.value.toString()
        }
    }
    
    /**
     * Convert map of flags to JSON-serializable map.
     * 
     * @param flags Map of flag keys to values
     * @return Map with JSON-serializable values
     */
    fun flagsToJson(flags: Map<FlagKey, FlagValue>): Map<String, Any?> {
        return flags.mapValues { (_, value) -> toJsonValue(value) }
    }
    
    /**
     * Parse type string and convert default value to appropriate type.
     * 
     * Used by REST controllers to parse query parameters.
     * 
     * @param type Type string (bool, int, double, string)
     * @param default Default value as string
     * @return Parsed value of appropriate type
     */
    fun parseTypedValue(type: String, default: String): Any {
        return when (type.lowercase()) {
            "bool", "boolean" -> default.toBoolean()
            "int", "integer" -> default.toInt()
            "double" -> default.toDouble()
            else -> default
        }
    }
}

