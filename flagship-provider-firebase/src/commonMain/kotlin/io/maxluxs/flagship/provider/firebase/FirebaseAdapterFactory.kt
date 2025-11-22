package io.maxluxs.flagship.provider.firebase

/**
 * Factory for creating platform-specific Firebase Remote Config adapters.
 * 
 * Note: Firebase SDK is only available on Android and iOS.
 * On JVM Desktop and Web, this will create adapters that throw
 * UnsupportedOperationException when used.
 */
expect object FirebaseAdapterFactory {
    /**
     * Creates a platform-specific Firebase Remote Config adapter.
     * 
     * @return FirebaseRemoteConfigAdapter instance
     */
    fun create(): FirebaseRemoteConfigAdapter
}

