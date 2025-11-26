package io.maxluxs.flagship.sample

/**
 * Configuration for provider selection
 */
enum class ProviderType {
    MOCK,
    REST,
    FIREBASE,
    LAUNCHDARKLY;
    
    val displayName: String get() = when(this) {
        MOCK -> "Mock (Demo)"
        REST -> "REST API"
        FIREBASE -> "Firebase Remote Config"
        LAUNCHDARKLY -> "LaunchDarkly"
    }
    
    val description: String get() = when(this) {
        MOCK -> "Local mock data for testing"
        REST -> "Custom REST backend"
        FIREBASE -> "Google Firebase Remote Config"
        LAUNCHDARKLY -> "Enterprise feature flags"
    }
    
    val requiresSetup: Boolean get() = when(this) {
        MOCK -> false
        REST, FIREBASE, LAUNCHDARKLY -> true
    }
}

/**
 * Stores selected provider across app lifecycle
 */
expect object ProviderPreferences {
    fun saveSelectedProvider(type: ProviderType)
    fun getSelectedProvider(): ProviderType
    fun clearProvider()
}


