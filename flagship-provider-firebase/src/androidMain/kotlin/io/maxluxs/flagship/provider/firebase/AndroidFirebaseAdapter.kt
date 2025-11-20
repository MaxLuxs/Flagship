package io.maxluxs.flagship.provider.firebase

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await

/**
 * Android implementation of Firebase Remote Config adapter.
 * Wraps the Firebase Android SDK.
 *
 * Usage:
 * ```kotlin
 * val firebaseConfig = FirebaseRemoteConfig.getInstance()
 * val adapter = AndroidFirebaseAdapter(firebaseConfig)
 * val provider = FirebaseRemoteConfigProvider(adapter)
 * ```
 */
class AndroidFirebaseAdapter(
    private val remoteConfig: FirebaseRemoteConfig
) : FirebaseRemoteConfigAdapter {

    override suspend fun fetchAndActivate() {
        remoteConfig.fetchAndActivate().await()
    }

    override suspend fun fetch() {
        remoteConfig.fetch().await()
    }

    override suspend fun activate() {
        remoteConfig.activate().await()
    }

    override fun getString(key: String): String {
        return remoteConfig.getString(key)
    }

    override fun getAllKeys(): Set<String> {
        return remoteConfig.all.keys
    }
}

