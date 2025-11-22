package io.maxluxs.flagship.provider.launchdarkly

import cocoapods.LaunchDarkly.LDClient
import cocoapods.LaunchDarkly.LDConfig
import cocoapods.LaunchDarkly.LDContextBuilder
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Factory for creating LaunchDarkly provider instances on iOS.
 * Handles LaunchDarkly SDK initialization and configuration.
 */
object LaunchDarklyProviderFactory {

    /**
     * Creates a LaunchDarkly provider with the specified mobile key.
     *
     * @param mobileKey LaunchDarkly mobile SDK key
     * @param userId Optional user ID (defaults to bundle identifier or timestamp-based ID)
     * @param userName Optional user name
     * @param name Provider name (default: "launchdarkly")
     * @param knownFlagKeys Optional list of known flag keys. If provided, these keys will be
     *                      explicitly fetched in getAllFlags(). Useful when LaunchDarkly SDK
     *                      doesn't provide a way to enumerate all flags.
     * @return Configured LaunchDarklyProvider instance
     */
    @OptIn(ExperimentalForeignApi::class, ExperimentalTime::class)
    fun create(
        mobileKey: String,
        userId: String? = null,
        userName: String? = null,
        name: String = "launchdarkly",
        knownFlagKeys: List<String>? = null
    ): LaunchDarklyProvider {
        // Create config
        val config = LDConfig(mobileKey = mobileKey)

        // Create context
        val contextKey = userId ?: NSBundle.mainBundle.bundleIdentifier
        ?: "user-${Clock.System.now().epochSeconds}"
        val contextBuilder = LDContextBuilder(key = contextKey)

        userName?.let { contextBuilder.nameWithName(it) }

        val buildResult = contextBuilder.build()
        // Extract LDContext from ContextBuilderResult
        val context = when {
            buildResult.failure() != null -> {
                // Fallback to simple context if build failed
                LDContextBuilder(key = contextKey).build().success()
                    ?: throw IllegalStateException("Failed to create LDContext")
            }

            else -> buildResult.success()
                ?: throw IllegalStateException("Failed to create LDContext")
        }

        // Initialize client (this starts the SDK)
        // Using startWithConfiguration with completion = null for synchronous initialization
        LDClient.startWithConfiguration(
            configuration = config,
            context = context,
            completion = null
        )

        val adapter = IOSLaunchDarklyAdapter()
        return LaunchDarklyProvider(adapter, name, knownFlagKeys)
    }
}

