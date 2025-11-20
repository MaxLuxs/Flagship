package io.maxluxs.flagship.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents a typed value for a feature flag.
 * 
 * Supports multiple types to allow flexible flag configurations:
 * - Boolean flags for simple on/off switches
 * - Numeric flags (Int, Double) for thresholds and limits
 * - String flags for configuration values
 * - JSON flags for complex structured data
 * 
 * Example:
 * ```kotlin
 * val boolFlag = FlagValue.Bool(true)
 * val intFlag = FlagValue.Int(100)
 * val stringFlag = FlagValue.StringV("blue")
 * ```
 */
@Serializable
sealed class FlagValue {
    /**
     * Boolean flag value.
     * 
     * @property value The boolean value of the flag
     */
    @Serializable
    data class Bool(val value: Boolean) : FlagValue()

    /**
     * Integer flag value.
     * 
     * @property value The integer value of the flag
     */
    @Serializable
    data class Int(val value: kotlin.Int) : FlagValue()

    /**
     * Double precision floating point flag value.
     * 
     * @property value The double value of the flag
     */
    @Serializable
    data class Double(val value: kotlin.Double) : FlagValue()

    /**
     * String flag value.
     * 
     * @property value The string value of the flag
     */
    @Serializable
    data class StringV(val value: String) : FlagValue()

    /**
     * JSON flag value for complex structured data.
     * 
     * @property value The JSON element containing structured data
     */
    @Serializable
    data class Json(val value: JsonElement) : FlagValue()

    /**
     * Safely cast to Boolean value.
     * 
     * @return The boolean value if this is a Bool flag, null otherwise
     */
    fun asBoolean(): Boolean? = (this as? Bool)?.value
    
    /**
     * Safely cast to Int value.
     * 
     * @return The integer value if this is an Int flag, null otherwise
     */
    fun asInt(): kotlin.Int? = (this as? Int)?.value
    
    /**
     * Safely cast to Double value.
     * 
     * @return The double value if this is a Double flag, null otherwise
     */
    fun asDouble(): kotlin.Double? = (this as? Double)?.value
    
    /**
     * Safely cast to String value.
     * 
     * @return The string value if this is a StringV flag, null otherwise
     */
    fun asString(): String? = (this as? StringV)?.value
    
    /**
     * Safely cast to JsonElement value.
     * 
     * @return The JSON element if this is a Json flag, null otherwise
     */
    fun asJson(): JsonElement? = (this as? Json)?.value
}

