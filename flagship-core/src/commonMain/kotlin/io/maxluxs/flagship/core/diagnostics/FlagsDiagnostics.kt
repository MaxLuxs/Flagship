package io.maxluxs.flagship.core.diagnostics

import io.maxluxs.flagship.core.model.FlagKey
import io.maxluxs.flagship.core.model.FlagValue

/**
 * Diagnostic information about the flags system.
 * 
 * Provides insights into the current state of flags, providers,
 * and configuration for debugging and monitoring.
 */
data class FlagsDiagnostics(
    /**
     * Application configuration info.
     */
    val appKey: String,
    val environment: String,
    
    /**
     * Provider information.
     */
    val providers: List<ProviderDiagnostics>,
    
    /**
     * Cache information.
     */
    val cacheStats: CacheStats,
    
    /**
     * Active flags count.
     */
    val totalFlags: Int,
    val totalExperiments: Int,
    
    /**
     * Override information.
     */
    val activeOverrides: Map<FlagKey, FlagValue>,
    
    /**
     * Last refresh time.
     */
    val lastRefreshMs: Long?,
    
    /**
     * System health.
     */
    val isHealthy: Boolean,
    val errors: List<String>
)

/**
 * Diagnostic information about a specific provider.
 */
data class ProviderDiagnostics(
    val name: String,
    val isActive: Boolean,
    val lastFetchMs: Long?,
    val revision: String?,
    val flagCount: Int,
    val experimentCount: Int,
    val errors: List<String>
)

/**
 * Cache statistics.
 */
data class CacheStats(
    val totalEntries: Int,
    val totalSizeBytes: Long,
    val oldestEntryMs: Long?,
    val newestEntryMs: Long?
)

/**
 * Provider for diagnostic information.
 */
interface DiagnosticsProvider {
    /**
     * Get current diagnostics snapshot.
     */
    fun getDiagnostics(): FlagsDiagnostics
    
    /**
     * Check if the system is healthy.
     * 
     * A system is considered healthy if:
     * - At least one provider is active
     * - No critical errors
     * - Configuration was successfully loaded
     */
    fun isHealthy(): Boolean
    
    /**
     * Get list of current issues/warnings.
     */
    fun getIssues(): List<DiagnosticIssue>
}

/**
 * A diagnostic issue or warning.
 */
data class DiagnosticIssue(
    val severity: Severity,
    val component: String,
    val message: String,
    val timestampMs: Long
) {
    enum class Severity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
}

