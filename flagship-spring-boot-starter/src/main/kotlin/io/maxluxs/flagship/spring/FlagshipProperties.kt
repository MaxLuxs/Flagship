package io.maxluxs.flagship.spring

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for Flagship.
 *
 * Example application.properties:
 * ```properties
 * flagship.app-key=my-app
 * flagship.environment=production
 * ```
 */
@ConfigurationProperties(prefix = "flagship")
data class FlagshipProperties(
    /**
     * Application key identifier.
     */
    val appKey: String? = null,

    /**
     * Environment name (production, staging, development).
     */
    val environment: String? = null
)

