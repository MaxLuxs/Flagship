package io.maxluxs.flagship.provider.launchdarkly

import cocoapods.LaunchDarkly.LDClient
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay

/**
 * iOS implementation of LaunchDarkly adapter.
 * Wraps the LaunchDarkly iOS SDK via Cocoapods.
 *
 * Usage:
 * ```kotlin
 * val config = LDConfig(mobileKey = "mob-YOUR-KEY")
 * val context = LDContextBuilder(key = "user-id", kind = "user").build()
 * LDClient.startWithConfiguration(configuration = config, context = context)
 * val client = LDClient.get()
 * val adapter = IOSLaunchDarklyAdapter()
 * val provider = LaunchDarklyProvider(adapter)
 * ```
 */
@OptIn(ExperimentalForeignApi::class)

class IOSLaunchDarklyAdapter : LaunchDarklyAdapter {

    private fun getClient(): LDClient? {
        return LDClient.get()
    }

    override suspend fun initialize() {
        // Wait for client to be initialized
        // LaunchDarkly iOS SDK initializes asynchronously via LDClient.start()
        var retries = 0
        while (retries < 50) {
            val ldClient = getClient()
            if (ldClient != null) {
                // Client is ready
                break
            }
            delay(100)
            retries++
        }
    }

    override suspend fun refresh() {
        // Force flush to get latest flags
        getClient()?.flush()
        delay(500) // Give it time to sync
    }

    override fun getAllFlags(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        // LaunchDarkly iOS SDK doesn't provide easy way to get all flags
        // We need to know flag keys upfront or use the streaming API
        // For now, return empty map - provider will rely on direct flag lookups
        // This matches the Android implementation behavior

        return result
    }

    override fun getRevision(): String? {
        // LaunchDarkly doesn't expose revision directly on iOS SDK
        return null
    }

    /**
     * Get a specific flag value by key
     */
    fun getBooleanValue(key: String, default: Boolean = false): Boolean {
        return getClient()?.boolVariationForKey(key = key, defaultValue = default) ?: default
    }

    fun getIntValue(key: String, default: Int = 0): Int {
        return getClient()?.integerVariationForKey(key = key, defaultValue = default.toLong())
            ?.toInt() ?: default
    }

    fun getDoubleValue(key: String, default: Double = 0.0): Double {
        return getClient()?.doubleVariationForKey(key = key, defaultValue = default) ?: default
    }

    fun getStringValue(key: String, default: String = ""): String {
        return getClient()?.stringVariationForKey(key = key, defaultValue = default) ?: default
    }

    fun getJsonValueAsString(key: String): String {
        return getClient()?.stringVariationForKey(key = key, defaultValue = "{}") ?: "{}"
    }
}

