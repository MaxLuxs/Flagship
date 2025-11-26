package io.maxluxs.flagship.core.errors

/**
 * Base exception for all Flagship-related errors.
 */
sealed class FlagshipException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * Error code for programmatic error handling.
     */
    abstract val errorCode: String
}

/**
 * Thrown when flags system is not configured.
 */
class NotConfiguredException(
    message: String = "Flagship not configured. Call Flagship.configure() first."
) : FlagshipException(message) {
    override val errorCode: String = "NOT_CONFIGURED"
}

/**
 * Thrown when bootstrap fails or times out.
 */
class BootstrapException(
    message: String,
    cause: Throwable? = null
) : FlagshipException(message, cause) {
    override val errorCode: String = "BOOTSTRAP_FAILED"
}

/**
 * Thrown when a provider fails to fetch data.
 */
class ProviderException(
    val providerName: String,
    message: String,
    cause: Throwable? = null
) : FlagshipException("Provider '$providerName': $message", cause) {
    override val errorCode: String = "PROVIDER_ERROR"
}

/**
 * Thrown when network request fails.
 */
class NetworkException(
    message: String,
    cause: Throwable? = null
) : FlagshipException(message, cause) {
    override val errorCode: String = "NETWORK_ERROR"
}

/**
 * Thrown when response parsing fails.
 */
class ParseException(
    message: String,
    cause: Throwable? = null
) : FlagshipException(message, cause) {
    override val errorCode: String = "PARSE_ERROR"
}

/**
 * Thrown when cache operation fails.
 */
class CacheException(
    message: String,
    cause: Throwable? = null
) : FlagshipException(message, cause) {
    override val errorCode: String = "CACHE_ERROR"
}

/**
 * Thrown when signature verification fails.
 */
class SignatureException(
    message: String,
    cause: Throwable? = null
) : FlagshipException(message, cause) {
    override val errorCode: String = "SIGNATURE_INVALID"
}

/**
 * Thrown when configuration is invalid.
 */
class ConfigurationException(
    message: String,
    cause: Throwable? = null
) : FlagshipException(message, cause) {
    override val errorCode: String = "CONFIGURATION_INVALID"
}

/**
 * Thrown when flag value type doesn't match expected type.
 */
class TypeMismatchException(
    val flagKey: String,
    val expectedType: String,
    val actualType: String
) : FlagshipException(
    "Flag '$flagKey' type mismatch: expected $expectedType, got $actualType"
) {
    override val errorCode: String = "TYPE_MISMATCH"
}

/**
 * Thrown when validation fails.
 */
class ValidationException(
    message: String,
    cause: Throwable? = null
) : FlagshipException(message, cause) {
    override val errorCode: String = "VALIDATION_ERROR"
}

