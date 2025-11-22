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

    override fun getAllFlags(knownKeys: List<String>?): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        
        // LaunchDarkly Android SDK 5.x doesn't provide easy way to get all flags
        // If knownKeys are provided, fetch them explicitly using direct lookups
        knownKeys?.forEach { key ->
            try {
                // Try different types - LaunchDarkly SDK methods return default if flag doesn't exist
                // We need to check if flag actually exists by comparing with a sentinel value
                
                // Try string first (most common for JSON experiments)
                val stringDefault = "__FLAGSHIP_NOT_FOUND__"
                val stringValue = client.stringVariation(key, stringDefault)
                if (stringValue != stringDefault && stringValue.isNotEmpty()) {
                    result[key] = stringValue
                } else {
                    // Try boolean
                    val boolDefault = false
                    val boolValue = client.boolVariation(key, boolDefault)
                    // Check if flag exists by trying opposite default
                    val boolCheck = client.boolVariation(key, !boolDefault)
                    if (boolValue != boolCheck) {
                        result[key] = boolValue
                    } else {
                        // Try integer
                        val intDefault = -999999
                        val intValue = client.intVariation(key, intDefault)
                        val intCheck = client.intVariation(key, intDefault + 1)
                        if (intValue != intCheck && intValue != intDefault) {
                            result[key] = intValue
                        } else {
                            // Try double
                            val doubleDefault = -999999.0
                            val doubleValue = client.doubleVariation(key, doubleDefault)
                            val doubleCheck = client.doubleVariation(key, doubleDefault + 1.0)
                            if (doubleValue != doubleCheck && doubleValue != doubleDefault) {
                                result[key] = doubleValue
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Flag doesn't exist or error - skip
            }
        }
        
        // If no knownKeys provided, return empty map - provider will rely on direct flag lookups
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

