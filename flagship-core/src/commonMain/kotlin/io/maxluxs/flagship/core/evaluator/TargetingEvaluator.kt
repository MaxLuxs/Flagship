package io.maxluxs.flagship.core.evaluator

import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.TargetingRule

/**
 * Evaluator for targeting rules.
 * 
 * Evaluates rules against an evaluation context to determine if a user
 * should see a feature or be included in an experiment.
 */
object TargetingEvaluator {
    /**
     * Evaluate a targeting rule against the provided context.
     * 
     * @param rule The targeting rule to evaluate
     * @param context The evaluation context with user/device information
     * @return true if the rule matches, false otherwise
     */
    fun evaluate(rule: TargetingRule, context: EvalContext): Boolean {
        return when (rule) {
            is TargetingRule.AttributeEquals -> {
                context.attributes[rule.key] == rule.value
            }
            is TargetingRule.AttributeIn -> {
                val value = context.attributes[rule.key]
                value != null && value in rule.values
            }
            is TargetingRule.RegionIn -> {
                context.region?.let { it in rule.regions } ?: false
            }
            is TargetingRule.AppVersionGte -> {
                compareVersions(context.appVersion, rule.version) >= 0
            }
            is TargetingRule.AppVersionLt -> {
                compareVersions(context.appVersion, rule.version) < 0
            }
            is TargetingRule.OsVersionGte -> {
                compareVersions(context.osVersion, rule.version) >= 0
            }
            is TargetingRule.UserIdIn -> {
                context.userId?.let { it in rule.userIds } ?: false
            }
            is TargetingRule.PercentageRollout -> {
                val id = context.userId ?: context.deviceId ?: return false
                BucketingEngine.isInBucket(id, rule.percent)
            }
            is TargetingRule.Composite -> {
                val allMatch = rule.all.isEmpty() || rule.all.all { evaluate(it, context) }
                val anyMatch = rule.any.isEmpty() || rule.any.any { evaluate(it, context) }
                allMatch && anyMatch
            }
        }
    }

    /**
     * Compare two version strings using semantic versioning.
     * 
     * Supports standard SemVer format: X.Y.Z[-prerelease]
     * Examples: 1.0.0, 2.1, 3.0.0-beta
     * 
     * @param version1 First version string
     * @param version2 Second version string
     * @return Negative if version1 < version2, 0 if equal, positive if version1 > version2
     */
    private fun compareVersions(version1: String, version2: String): Int {
        // Split into main version and prerelease (e.g. "1.2.3" and "beta.1")
        val v1Parts = version1.split("-", limit = 2)
        val v2Parts = version2.split("-", limit = 2)

        val v1Main = v1Parts[0].split(".").map { it.toIntOrNull() ?: 0 }
        val v2Main = v2Parts[0].split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(v1Main.size, v2Main.size)

        for (i in 0 until maxLength) {
            val v1 = v1Main.getOrNull(i) ?: 0
            val v2 = v2Main.getOrNull(i) ?: 0

            if (v1 != v2) {
                return v1.compareTo(v2)
            }
        }

        // Main parts are equal. Check prerelease.
        // Version without prerelease is GREATER than version with prerelease.
        val v1Pre = v1Parts.getOrNull(1)
        val v2Pre = v2Parts.getOrNull(1)

        if (v1Pre == null && v2Pre == null) return 0
        if (v1Pre == null) return 1 // v1 (release) > v2 (prerelease)
        if (v2Pre == null) return -1 // v1 (prerelease) < v2 (release)

        return v1Pre.compareTo(v2Pre)
    }
}

