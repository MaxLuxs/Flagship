package io.maxluxs.flagship.provider.firebase

actual object FirebaseAdapterFactory {
    actual fun create(): FirebaseRemoteConfigAdapter {
        return JvmFirebaseAdapter()
    }
}

