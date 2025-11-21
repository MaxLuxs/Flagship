package io.maxluxs.flagship.provider.launchdarkly

import cocoapods.LaunchDarkly.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import platform.Foundation.NSBundle

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
     * @return Configured LaunchDarklyProvider instance
     */
    @OptIn(ExperimentalForeignApi::class, ExperimentalTime::class)
    fun create(
        mobileKey: String,
        userId: String? = null,
        userName: String? = null,
        name: String = "launchdarkly"
    ): LaunchDarklyProvider {
        // Create config
        val config = LDConfig(mobileKey = mobileKey)

        // Create context
        val contextKey = userId ?: NSBundle.mainBundle.bundleIdentifier ?: "user-${Clock.System.now().epochSeconds}"
        val contextBuilder = LDContextBuilder(key = contextKey)

        userName?.let { contextBuilder.nameWithName(it) }

        val buildResult = contextBuilder.build()
        // Extract LDContext from ContextBuilderResult
        // ContextBuilderResult has a context property or method to get the LDContext
        val context = if (buildResult.isError()) {
            // Fallback to simple context if build failed
            val fallbackBuilder = LDContextBuilder(key = contextKey)
            val fallbackResult = fallbackBuilder.build()
            fallbackResult.context ?: throw IllegalStateException("Failed to create LDContext")
        } else {
            buildResult.context ?: throw IllegalStateException("Failed to create LDContext")
        }

        // Initialize client (this starts the SDK)
        // Using startWithConfiguration with completion = null for synchronous initialization
        LDClient.startWithConfiguration(
            configuration = config,
            context = context,
            completion = null
        )

        val adapter = IOSLaunchDarklyAdapter()
        return LaunchDarklyProvider(adapter, name)
    }
}

