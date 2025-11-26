package io.maxluxs.flagship.core.config

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.errors.ConfigurationException
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.provider.VersionedProvider
import io.maxluxs.flagship.core.provider.isVersionInRange

/**
 * Validates FlagsConfig for common issues.
 */
object ConfigValidator {
    /**
     * Validate configuration and throw exception if invalid.
     * 
     * @param config The configuration to validate
     * @throws ConfigurationException if configuration is invalid
     */
    fun validateOrThrow(config: FlagsConfig) {
        val issues = validate(config)
        if (issues.isNotEmpty()) {
            throw ConfigurationException(
                "Configuration validation failed:\n" + issues.joinToString("\n")
            )
        }
    }
    
    /**
     * Validate configuration and return list of issues.
     * 
     * @param config The configuration to validate
     * @return List of validation issues (empty if valid)
     */
    fun validate(config: FlagsConfig): List<String> {
        val issues = mutableListOf<String>()
        
        // Validate appKey
        if (config.appKey.isBlank()) {
            issues.add("appKey cannot be blank")
        }
        
        // Validate environment
        if (config.environment.isBlank()) {
            issues.add("environment cannot be blank")
        }
        
        // Validate providers
        if (config.providers.isEmpty()) {
            issues.add("At least one provider is required")
        }
        
        // Check for duplicate provider names
        val providerNames = config.providers.map { it.name }
        val duplicates = providerNames.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            issues.add("Duplicate provider names: ${duplicates.keys.joinToString()}")
        }
        
        // Validate provider versions
        val coreVersion = getCoreVersion()
        config.providers.forEach { provider ->
            if (provider is VersionedProvider) {
                val minVersion = provider.minCoreVersion
                val maxVersion = provider.maxCoreVersion
                
                if (minVersion != null || maxVersion != null) {
                    val inRange = isVersionInRange(coreVersion, minVersion, maxVersion)
                    when {
                        inRange == null -> {
                            issues.add("Warning: Invalid version format for provider '${provider.name}' (core: $coreVersion, min: $minVersion, max: $maxVersion)")
                        }
                        !inRange -> {
                            val range = when {
                                minVersion != null && maxVersion != null -> "$minVersion - $maxVersion"
                                minVersion != null -> ">= $minVersion"
                                maxVersion != null -> "<= $maxVersion"
                                else -> ""
                            }
                            issues.add("Provider '${provider.name}' version ${provider.version} requires Flagship core version $range, but current version is $coreVersion")
                        }
                    }
                }
            }
        }
        
        // Validate refresh interval
        if (config.defaultRefreshIntervalMs < 0) {
            issues.add("defaultRefreshIntervalMs must be non-negative")
        }
        
        // Warn about potential issues
        if (config.providers.size > 5) {
            issues.add("Warning: Too many providers (${config.providers.size}) may impact performance")
        }
        
        if (config.defaultRefreshIntervalMs < 60_000 && config.defaultRefreshIntervalMs > 0) {
            issues.add("Warning: Very short refresh interval (${config.defaultRefreshIntervalMs}ms) may cause excessive network usage")
        }
        
        return issues
    }
    
    /**
     * Get warnings about configuration (non-critical issues).
     * 
     * @param config The configuration to check
     * @return List of warnings
     */
    fun getWarnings(config: FlagsConfig): List<String> {
        val warnings = mutableListOf<String>()
        
        if (config.cache is io.maxluxs.flagship.core.cache.InMemoryCache && config.providers.isNotEmpty()) {
            warnings.add("Using InMemoryCache - data will be lost on app restart. Consider using PersistentCache for production.")
        }
        
        if (config.analytics is io.maxluxs.flagship.core.analytics.NoopAnalytics) {
            warnings.add("No analytics configured - experiment tracking will not work")
        }
        
        if (config.retryPolicy is io.maxluxs.flagship.core.retry.NoRetryPolicy) {
            warnings.add("No retry policy configured - network failures will not be retried")
        }
        
        // Warn about versioned providers without version info
        config.providers.forEach { provider ->
            if (provider !is VersionedProvider) {
                warnings.add("Provider '${provider.name}' does not implement VersionedProvider - version compatibility cannot be checked")
            }
        }
        
        return warnings
    }
    
    /**
     * Get current Flagship core version.
     * 
     * This should match the library version.
     * 
     * @return Version string (e.g., "0.1.0")
     */
    private fun getCoreVersion(): String {
        // Try to get from resources/manifest, fallback to default
        return try {
            // In a real implementation, this would read from build config or manifest
            // For now, return a default version
            "0.1.0"
        } catch (e: Exception) {
            "0.1.0"
        }
    }
}

