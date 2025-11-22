package io.maxluxs.flagship.provider.firebase

import cocoapods.FirebaseRemoteConfig.FIRRemoteConfig

actual object FirebaseAdapterFactory {
    actual fun create(): FirebaseRemoteConfigAdapter {
        val remoteConfig = FIRRemoteConfig.remoteConfig()
        return IOSFirebaseAdapter(remoteConfig)
    }
}

