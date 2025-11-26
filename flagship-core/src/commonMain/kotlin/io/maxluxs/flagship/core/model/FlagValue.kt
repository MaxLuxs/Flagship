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
 * - Date/DateTime flags for time-based features
 * - Enum flags for predefined values
 * - List/Array flags for collections
 * - Map/Object flags for key-value pairs
 * 
 * Example:
 * ```kotlin
 * val boolFlag = FlagValue.Bool(true)
 * val intFlag = FlagValue.Int(100)
 * val stringFlag = FlagValue.StringV("blue")
 * val dateFlag = FlagValue.Date(1234567890L)
 * val listFlag = FlagValue.List(listOf("a", "b", "c"))
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
     * Date/DateTime flag value (timestamp in milliseconds since epoch).
     * 
     * @property value Timestamp in milliseconds
     */
    @Serializable
    data class Date(val value: Long) : FlagValue()
    
    /**
     * Enum flag value for predefined string values.
     * 
     * @property value The enum string value
     */
    @Serializable
    data class Enum(val value: String) : FlagValue()
    
    /**
     * List/Array flag value for collections.
     * 
     * @property value List of flag values
     */
    @Serializable
    data class List(val value: kotlin.collections.List<FlagValue>) : FlagValue()
    
    /**
     * Map/Object flag value for key-value pairs.
     * 
     * @property value Map of string keys to flag values
     */
    @Serializable
    data class Map(val value: kotlin.collections.Map<String, FlagValue>) : FlagValue()

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
    
    /**
     * Safely cast to Date value (timestamp in milliseconds).
     * 
     * @return The timestamp if this is a Date flag, null otherwise
     */
    fun asDate(): Long? = (this as? Date)?.value
    
    /**
     * Safely cast to Enum value.
     * 
     * @return The enum string if this is an Enum flag, null otherwise
     */
    fun asEnum(): String? = (this as? Enum)?.value
    
    /**
     * Safely cast to List value.
     * 
     * @return The list if this is a List flag, null otherwise
     */
    fun asList(): kotlin.collections.List<FlagValue>? = (this as? List)?.value
    
    /**
     * Safely cast to Map value.
     * 
     * @return The map if this is a Map flag, null otherwise
     */
    fun asMap(): kotlin.collections.Map<String, FlagValue>? = (this as? Map)?.value
}

