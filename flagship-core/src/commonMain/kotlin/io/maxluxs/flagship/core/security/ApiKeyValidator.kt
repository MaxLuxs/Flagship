package io.maxluxs.flagship.core.security

/**
 * Validates API keys and checks permissions.
 * 
 * In production, this would typically query a database or key management service.
 * For MVP, we provide an in-memory implementation.
 */
interface ApiKeyValidator {
    /**
     * Validate an API key and return its metadata.
     * 
     * @param apiKey The API key to validate
     * @return ApiKey if valid, null if invalid or expired
     */
    fun validate(apiKey: String): ApiKey?
    
    /**
     * Check if an API key has read permission.
     */
    fun canRead(apiKey: String): Boolean {
        return validate(apiKey)?.canRead() ?: false
    }
    
    /**
     * Check if an API key has write permission.
     */
    fun canWrite(apiKey: String): Boolean {
        return validate(apiKey)?.canWrite() ?: false
    }
}

/**
 * In-memory API key validator for MVP/testing.
 * 
 * In production, replace with a database-backed implementation.
 */
class InMemoryApiKeyValidator(
    private val keys: Map<String, ApiKey> = emptyMap()
) : ApiKeyValidator {
    
    override fun validate(apiKey: String): ApiKey? {
        val key = keys[apiKey] ?: return null
        
        if (key.isExpired()) {
            return null
        }
        
        return key
    }
    
    companion object {
        /**
         * Create a validator with common test keys.
         */
        fun withTestKeys(): InMemoryApiKeyValidator {
            return InMemoryApiKeyValidator(
                mapOf(
                    "sk_test_readonly" to ApiKey(
                        key = "sk_test_readonly",
                        role = ApiKeyRole.READ_ONLY,
                        appKey = "test-app"
                    ),
                    "sk_test_admin" to ApiKey(
                        key = "sk_test_admin",
                        role = ApiKeyRole.ADMIN,
                        appKey = "test-app"
                    )
                )
            )
        }
    }
}

/**
 * No-op validator that allows all keys (for development only).
 */
object NoopApiKeyValidator : ApiKeyValidator {
    override fun validate(apiKey: String): ApiKey? {
        return ApiKey(
            key = apiKey,
            role = ApiKeyRole.ADMIN,
            appKey = "dev"
        )
    }
}

