package io.maxluxs.flagship.provider.firebase

import cocoapods.FirebaseRemoteConfig.FIRRemoteConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS implementation of Firebase Remote Config adapter.
 * Wraps the Firebase iOS SDK via Cocoapods.
 *
 * Usage:
 * ```kotlin
 * val firebaseConfig = FIRRemoteConfig.remoteConfig()
 * val adapter = IOSFirebaseAdapter(firebaseConfig)
 * val provider = FirebaseRemoteConfigProvider(adapter)
 * ```
 */
class IOSFirebaseAdapter(
    private val remoteConfig: FIRRemoteConfig
) : FirebaseRemoteConfigAdapter {

    override suspend fun fetchAndActivate() {
        suspendCancellableCoroutine { continuation ->
            remoteConfig.fetchAndActivateWithCompletionHandler { status, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                } else {
                    continuation.resume(Unit)
                }
            }
        }
    }

    override suspend fun fetch() {
        suspendCancellableCoroutine { continuation ->
            remoteConfig.fetchWithCompletionHandler { error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                } else {
                    continuation.resume(Unit)
                }
            }
        }
    }

    override suspend fun activate() {
        suspendCancellableCoroutine { continuation ->
            remoteConfig.activateWithCompletion { _, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                } else {
                    continuation.resume(Unit)
                }
            }
        }
    }

    override fun getString(key: String): String {
        return remoteConfig.configValueForKey(key)?.stringValue ?: ""
    }

    override fun getAllKeys(): Set<String> {
        return remoteConfig.allKeysFromSource(
            source = cocoapods.FirebaseRemoteConfig.FIRRemoteConfigSourceRemote
        ).filterIsInstance<String>().toSet()
    }
}

