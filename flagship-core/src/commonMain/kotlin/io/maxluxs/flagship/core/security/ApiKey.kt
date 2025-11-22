package io.maxluxs.flagship.core.security

import io.maxluxs.flagship.core.util.currentTimeMillis

/**
 * API key with permissions for Flagship.
 * 
 * API keys can have different roles:
 * - READ_ONLY: Can only read flags (for client apps)
 * - ADMIN: Can read and modify flags (for backend/admin)
 * 
 * Example:
 * ```kotlin
 * val apiKey = ApiKey(
 *     key = "sk_live_abc123",
 *     role = ApiKeyRole.READ_ONLY,
 *     appKey = "my-app"
 * )
 * ```
 */
data class ApiKey(
    /**
     * The API key value (e.g., "sk_live_abc123").
     */
    val key: String,
    
    /**
     * Role/permissions for this API key.
     */
    val role: ApiKeyRole,
    
    /**
     * Application key this API key belongs to.
     */
    val appKey: String,
    
    /**
     * Optional description.
     */
    val description: String? = null,
    
    /**
     * Optional expiration timestamp (null = never expires).
     */
    val expiresAt: Long? = null
) {
    /**
     * Check if this API key is expired.
     */
    fun isExpired(currentTime: Long = currentTimeMillis()): Boolean {
        return expiresAt != null && currentTime > expiresAt
    }
    
    /**
     * Check if this API key can read flags.
     */
    fun canRead(): Boolean {
        return role == ApiKeyRole.READ_ONLY || role == ApiKeyRole.ADMIN
    }
    
    /**
     * Check if this API key can write/modify flags.
     */
    fun canWrite(): Boolean {
        return role == ApiKeyRole.ADMIN
    }
}

/**
 * Role/permissions for API keys.
 */
enum class ApiKeyRole {
    /**
     * Read-only access. Can only read flags and experiments.
     * Used for client applications.
     */
    READ_ONLY,
    
    /**
     * Full admin access. Can read and modify flags/experiments.
     * Used for backend services and admin tools.
     */
    ADMIN
}

