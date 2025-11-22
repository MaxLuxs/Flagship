package io.maxluxs.flagship.core.util

import io.maxluxs.flagship.core.model.FlagValue

/**
 * Utility for parsing flag values from various formats.
 * 
 * Used by multiple providers to avoid code duplication.
 */
object FlagValueParser {
    /**
     * Parse flag value from string.
     * 
     * Attempts to parse as boolean, int, double, or falls back to string.
     * 
     * @param value String value to parse
     * @return FlagValue or null if value is blank
     */
    fun parseFromString(value: String): FlagValue? {
        if (value.isBlank()) return null
        
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
    
    /**
     * Parse flag value from Any (dynamic type).
     * 
     * Used when parsing from JSON or other dynamic sources.
     * 
     * @param value Value to parse
     * @return FlagValue or null if type is not supported
     */
    fun parseFromAny(value: Any?): FlagValue? {
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
}

