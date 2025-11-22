package io.maxluxs.flagship.provider.firebase

/**
 * JVM Desktop implementation of Firebase Remote Config adapter.
 * 
 * Note: Firebase SDK is not available for JVM Desktop.
 * This adapter throws UnsupportedOperationException to indicate
 * that Firebase provider is only available on Android and iOS.
 */
class JvmFirebaseAdapter : FirebaseRemoteConfigAdapter {
    override suspend fun fetchAndActivate() {
        throw UnsupportedOperationException(
            "Firebase Remote Config is not supported on JVM Desktop. " +
            "Please use REST provider or other providers that support JVM."
        )
    }

    override suspend fun fetch() {
        throw UnsupportedOperationException(
            "Firebase Remote Config is not supported on JVM Desktop. " +
            "Please use REST provider or other providers that support JVM."
        )
    }

    override suspend fun activate() {
        throw UnsupportedOperationException(
            "Firebase Remote Config is not supported on JVM Desktop. " +
            "Please use REST provider or other providers that support JVM."
        )
    }

    override fun getString(key: String): String {
        throw UnsupportedOperationException(
            "Firebase Remote Config is not supported on JVM Desktop. " +
            "Please use REST provider or other providers that support JVM."
        )
    }

    override fun getAllKeys(): Set<String> {
        throw UnsupportedOperationException(
            "Firebase Remote Config is not supported on JVM Desktop. " +
            "Please use REST provider or other providers that support JVM."
        )
    }
}

