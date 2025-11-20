package io.maxluxs.flagship.provider.launchdarkly

import android.app.Application
import com.launchdarkly.sdk.LDContext
import com.launchdarkly.sdk.android.LDClient
import com.launchdarkly.sdk.android.LDConfig
import java.util.concurrent.Future

/**
 * Factory for creating LaunchDarkly provider instances on Android.
 * Handles LaunchDarkly SDK initialization and configuration.
 */
object LaunchDarklyProviderFactory {
    
    /**
     * Creates a LaunchDarkly provider with the specified mobile key.
     * 
     * @param application Android Application instance
     * @param mobileKey LaunchDarkly mobile SDK key
     * @param userId Optional user ID (defaults to timestamp-based ID)
     * @param userName Optional user name
     * @param name Provider name (default: "launchdarkly")
     * @return Configured LaunchDarklyProvider instance
     */
    fun create(
        application: Application,
        mobileKey: String,
        userId: String? = null,
        userName: String? = null,
        name: String = "launchdarkly"
    ): LaunchDarklyProvider {
        val config = LDConfig.Builder(LDConfig.Builder.AutoEnvAttributes.Disabled)
            .mobileKey(mobileKey)
            .build()
        
        val contextBuilder = LDContext.builder(userId ?: "user-${System.currentTimeMillis()}")
            .kind("user")
        
        userName?.let { contextBuilder.name(it) }
        
        val context = contextBuilder.build()
        
        val future: Future<LDClient> = LDClient.init(application, config, context)
        val ldClient = future.get() // Block until client is initialized
        
        val adapter = AndroidLaunchDarklyAdapter(ldClient)
        return LaunchDarklyProvider(adapter, name)
    }
}

