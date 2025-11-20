package io.maxluxs.flagship.provider.launchdarkly

import com.launchdarkly.sdk.android.LDClient
import kotlinx.coroutines.delay

/**
 * Android implementation of LaunchDarkly adapter.
 * Wraps the LaunchDarkly Android SDK.
 *
 * Usage:
 * ```kotlin
 * val ldClient = LDClient.init(
 *     application,
 *     LDConfig.Builder()
 *         .mobileKey("mob-YOUR-KEY")
 *         .build(),
 *     LDContext.builder("user-id").build()
 * )
 * val adapter = AndroidLaunchDarklyAdapter(ldClient)
 * val provider = LaunchDarklyProvider(adapter)
 * ```
 */
class AndroidLaunchDarklyAdapter(
    private val client: LDClient
) : LaunchDarklyAdapter {

    override suspend fun initialize() {
        // Wait for client to be initialized
        var retries = 0
        while (!client.isInitialized && retries < 50) {
            delay(100)
            retries++
        }
    }

    override suspend fun refresh() {
        // Force flush to get latest flags
        client.flush()
        delay(500) // Give it time to sync
    }

    override fun getAllFlags(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        
        // Note: LaunchDarkly Android SDK 5.x doesn't provide easy way to get all flags
        // We need to know flag keys upfront or use the streaming API
        // For now, return empty map - provider will rely on direct flag lookups
        
        return result
    }

    override fun getRevision(): String? {
        // LaunchDarkly doesn't expose revision directly on Android SDK
        return null
    }
    
    /**
     * Get a specific flag value by key
     */
    fun getBooleanValue(key: String, default: Boolean = false): Boolean {
        return client.boolVariation(key, default)
    }
    
    fun getIntValue(key: String, default: Int = 0): Int {
        return client.intVariation(key, default)
    }
    
    fun getDoubleValue(key: String, default: Double = 0.0): Double {
        return client.doubleVariation(key, default)
    }
    
    fun getStringValue(key: String, default: String = ""): String {
        return client.stringVariation(key, default)
    }
    
    fun getJsonValueAsString(key: String): String {
        return client.stringVariation(key, "{}")
    }
}

