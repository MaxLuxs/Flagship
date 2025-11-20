package io.maxluxs.flagship.core.model

import kotlinx.serialization.Serializable

/**
 * Targeting rule for conditional flag/experiment activation.
 * 
 * Rules are evaluated against an [EvalContext] to determine if a user
 * should see a feature or be included in an experiment.
 * 
 * Example:
 * ```kotlin
 * // Simple region targeting
 * val rule = TargetingRule.RegionIn(setOf("US", "CA"))
 * 
 * // Complex composite rule
 * val complexRule = TargetingRule.Composite(
 *     all = listOf(
 *         TargetingRule.AttributeEquals("tier", "gold"),
 *         TargetingRule.AppVersionGte("2.5.0")
 *     )
 * )
 * ```
 */
@Serializable
sealed class TargetingRule {
    /**
     * Rule that matches when a custom attribute equals a specific value.
     * 
     * @property key The attribute key from EvalContext.attributes
     * @property value The expected value for the attribute
     */
    @Serializable
    data class AttributeEquals(val key: String, val value: String) : TargetingRule()

    /**
     * Rule that matches when user's region is in the specified set.
     * 
     * @property regions Set of region/country codes (e.g., "US", "UK", "FR")
     */
    @Serializable
    data class RegionIn(val regions: Set<String>) : TargetingRule()

    /**
     * Rule that matches when app version is greater than or equal to specified version.
     * 
     * Supports semantic versioning (e.g., "2.5.0", "1.0.0").
     * 
     * @property version Minimum required app version
     */
    @Serializable
    data class AppVersionGte(val version: String) : TargetingRule()

    /**
     * Rule that matches when app version is less than specified version.
     * 
     * Useful for deprecating features in old versions.
     * 
     * @property version Maximum app version (exclusive)
     */
    @Serializable
    data class AppVersionLt(val version: String) : TargetingRule()

    /**
     * Rule that matches when user ID is in the specified set.
     * 
     * Useful for beta testing with specific users.
     * 
     * @property userIds Set of user IDs to match
     */
    @Serializable
    data class UserIdIn(val userIds: Set<String>) : TargetingRule()

    /**
     * Rule that matches a percentage of users based on deterministic hashing.
     * 
     * Useful for gradual rollouts (e.g., "show to 25% of users").
     * 
     * @property percent Percentage of users to match (0-100)
     */
    @Serializable
    data class PercentageRollout(val percent: Int) : TargetingRule() {
        init {
            require(percent in 0..100) { "Percentage must be between 0 and 100" }
        }
    }

    /**
     * Rule that matches when device OS version is greater than or equal to specified version.
     * 
     * @property version Minimum OS version
     */
    @Serializable
    data class OsVersionGte(val version: String) : TargetingRule()

    /**
     * Rule that matches based on custom attribute containing a value.
     * 
     * @property key The attribute key
     * @property values Set of possible values to match
     */
    @Serializable
    data class AttributeIn(val key: String, val values: Set<String>) : TargetingRule()

    /**
     * Composite rule that combines multiple rules with AND/OR logic.
     * 
     * - `all` rules must match (AND logic)
     * - At least one `any` rule must match (OR logic)
     * - If both are present, both conditions must be satisfied
     * 
     * @property all List of rules that must all match (AND)
     * @property any List of rules where at least one must match (OR)
     */
    @Serializable
    data class Composite(
        val all: List<TargetingRule> = emptyList(),
        val any: List<TargetingRule> = emptyList()
    ) : TargetingRule()
}

