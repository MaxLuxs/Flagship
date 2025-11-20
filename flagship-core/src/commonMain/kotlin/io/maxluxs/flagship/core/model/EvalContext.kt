package io.maxluxs.flagship.core.model

import kotlinx.serialization.Serializable

/**
 * Evaluation context for feature flag and experiment evaluation.
 * 
 * Contains user and device information used for:
 * - Targeting rules evaluation
 * - Deterministic bucketing in experiments
 * - Analytics and tracking
 * 
 * **Privacy Note**: Avoid including PII (Personally Identifiable Information).
 * Use pseudonymous IDs for userId and deviceId.
 * 
 * Example:
 * ```kotlin
 * val context = EvalContext(
 *     userId = "user_abc123",
 *     deviceId = "device_xyz789",
 *     appVersion = "2.5.0",
 *     osName = "Android",
 *     osVersion = "12",
 *     region = "US",
 *     attributes = mapOf("tier" to "gold", "segment" to "premium")
 * )
 * ```
 * 
 * @property userId Stable pseudonymous user identifier for consistent bucketing
 * @property deviceId Platform-specific device identifier
 * @property appVersion Application version string (e.g., "2.5.0")
 * @property osName Operating system name (e.g., "Android", "iOS")
 * @property osVersion Operating system version
 * @property locale User locale (e.g., "en_US", "fr_FR")
 * @property region User region/country code (e.g., "US", "UK", "FR")
 * @property attributes Custom key-value attributes for advanced targeting (e.g., tier, segment)
 */
@Serializable
data class EvalContext(
    val userId: String? = null,
    val deviceId: String? = null,
    val appVersion: String,
    val osName: String,
    val osVersion: String,
    val locale: String? = null,
    val region: String? = null,
    val attributes: Map<String, String> = emptyMap()
)

