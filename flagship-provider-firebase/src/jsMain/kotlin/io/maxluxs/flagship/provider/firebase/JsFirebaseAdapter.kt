package io.maxluxs.flagship.provider.firebase

/**
 * Web/JS implementation of Firebase Remote Config adapter.
 * 
 * Note: Firebase SDK for Web uses JavaScript SDK which is not directly
 * compatible with Kotlin/JS multiplatform. This adapter throws
 * UnsupportedOperationException to indicate that Firebase provider
 * is only available on Android and iOS.
 * 
 * For Web, consider using REST provider or Firebase REST API directly.
 */
class JsFirebaseAdapter : FirebaseRemoteConfigAdapter {
    override suspend fun fetchAndActivate() {
        throw UnsupportedOperationException(
            "Firebase Remote Config is not supported on Web/JS. " +
            "Please use REST provider or Firebase REST API directly."
        )
    }

    override suspend fun fetch() {
        throw UnsupportedOperationException(
            "Firebase Remote Config is not supported on Web/JS. " +
            "Please use REST provider or Firebase REST API directly."
        )
    }

    override suspend fun activate() {
        throw UnsupportedOperationException(
            "Firebase Remote Config is not supported on Web/JS. " +
            "Please use REST provider or Firebase REST API directly."
        )
    }

    override fun getString(key: String): String {
        throw UnsupportedOperationException(
            "Firebase Remote Config is not supported on Web/JS. " +
            "Please use REST provider or Firebase REST API directly."
        )
    }

    override fun getAllKeys(): Set<String> {
        throw UnsupportedOperationException(
            "Firebase Remote Config is not supported on Web/JS. " +
            "Please use REST provider or Firebase REST API directly."
        )
    }
}

