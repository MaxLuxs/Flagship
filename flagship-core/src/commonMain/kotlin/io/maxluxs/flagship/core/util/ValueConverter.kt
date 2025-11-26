package io.maxluxs.flagship.core.util

import io.maxluxs.flagship.core.model.FlagValue

/**
 * Utility for converting between FlagValue and typed values.
 * 
 * Eliminates code duplication between value() and valueSync() methods.
 * 
 * @see io.maxluxs.flagship.core.manager.DefaultFlagsManager
 */
object ValueConverter {
    /**
     * Convert default value to FlagValue.
     * 
     * Supports Boolean, Int, Double, and String types.
     * 
     * @param default The default value to convert
     * @return FlagValue representation, or null if type is not supported
     */
    fun <T> toFlagValue(default: T): FlagValue? {
        return when (default) {
            is Boolean -> FlagValue.Bool(default)
            is Int -> FlagValue.Int(default)
            is Double -> FlagValue.Double(default)
            is String -> FlagValue.StringV(default)
            else -> null
        }
    }
    
    /**
     * Convert FlagValue to typed value.
     * 
     * Converts FlagValue back to the original type based on the default value's type.
     * 
     * @param value The FlagValue to convert (can be null)
     * @param default The default value (type is inferred from this)
     * @return Converted value, or default if value is null or type mismatch
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> fromFlagValue(value: FlagValue?, default: T): T {
        return when (default) {
            is Boolean -> (value?.asBoolean() ?: default) as T
            is Int -> (value?.asInt() ?: default) as T
            is Double -> (value?.asDouble() ?: default) as T
            is String -> (value?.asString() ?: default) as T
            else -> default
        }
    }
}

