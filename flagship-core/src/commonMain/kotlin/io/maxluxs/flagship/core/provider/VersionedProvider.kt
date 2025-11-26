package io.maxluxs.flagship.core.provider

/**
 * Optional interface for providers that support version information.
 * 
 * Providers can implement this interface to provide version information
 * for compatibility checking and validation.
 * 
 * Example:
 * ```kotlin
 * class MyProvider : FlagsProvider, VersionedProvider {
 *     override val name = "my-provider"
 *     override val version = "1.2.3"
 *     override val minCoreVersion = "0.1.0"
 *     // ... other methods
 * }
 * ```
 */
interface VersionedProvider {
    /**
     * Provider version (e.g., "1.2.3").
     */
    val version: String
    
    /**
     * Minimum required Flagship core version (e.g., "0.1.0").
     * 
     * If null, no version check is performed.
     */
    val minCoreVersion: String? get() = null
    
    /**
     * Maximum supported Flagship core version (e.g., "1.0.0").
     * 
     * If null, no upper bound check is performed.
     */
    val maxCoreVersion: String? get() = null
}

/**
 * Parse version string to comparable format.
 * 
 * Supports semantic versioning (e.g., "1.2.3").
 * 
 * @param version Version string
 * @return List of version parts [major, minor, patch] or null if invalid
 */
internal fun parseVersion(version: String): List<Int>? {
    val parts = version.split(".")
    if (parts.size < 2 || parts.size > 3) return null
    
    return try {
        parts.map { it.toInt() }
    } catch (e: NumberFormatException) {
        null
    }
}

/**
 * Compare two version strings.
 * 
 * @param v1 First version
 * @param v2 Second version
 * @return Negative if v1 < v2, positive if v1 > v2, 0 if equal, null if invalid
 */
internal fun compareVersions(v1: String, v2: String): Int? {
    val parts1 = parseVersion(v1) ?: return null
    val parts2 = parseVersion(v2) ?: return null
    
    // Pad to same length
    val maxLen = maxOf(parts1.size, parts2.size)
    val p1 = parts1 + List(maxLen - parts1.size) { 0 }
    val p2 = parts2 + List(maxLen - parts2.size) { 0 }
    
    for (i in p1.indices) {
        val diff = p1[i] - p2[i]
        if (diff != 0) return diff
    }
    
    return 0
}

/**
 * Check if version is within range.
 * 
 * @param version Version to check
 * @param minVersion Minimum version (inclusive, null = no minimum)
 * @param maxVersion Maximum version (inclusive, null = no maximum)
 * @return true if version is within range, false otherwise, null if invalid format
 */
internal fun isVersionInRange(
    version: String,
    minVersion: String?,
    maxVersion: String?
): Boolean? {
    val parsed = parseVersion(version) ?: return null
    
    if (minVersion != null) {
        val min = compareVersions(version, minVersion) ?: return null
        if (min < 0) return false
    }
    
    if (maxVersion != null) {
        val max = compareVersions(version, maxVersion) ?: return null
        if (max > 0) return false
    }
    
    return true
}

