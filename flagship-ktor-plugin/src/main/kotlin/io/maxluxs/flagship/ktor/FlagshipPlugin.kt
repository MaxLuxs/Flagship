package io.maxluxs.flagship.ktor

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.FlagsConfigBuilder

/**
 * Ktor plugin for Flagship feature flags.
 * 
 * Usage:
 * ```kotlin
 * fun Application.module() {
 *     install(FlagshipPlugin) {
 *         appKey = "my-app"
 *         environment = "production"
 *         providers = listOf(restProvider)
 *     }
 *     
 *     routing {
 *         // Use Flagship in routes
 *         get("/") {
 *             val enabled = Flagship.isEnabled("new_ui")
 *             // ...
 *         }
 *     }
 * }
 * ```
 */
class FlagshipPlugin(config: Configuration) {
    val flagsManager: FlagsManager = config.build()
    
    class Configuration {
        var appKey: String = "ktor-app"
        var environment: String = "production"
        var providers: List<FlagsProvider> = emptyList()
        
        internal fun build(): FlagsManager {
            val config = FlagsConfigBuilder.build(
                appKey = appKey,
                environment = environment,
                providers = providers
            )
            
            return FlagsConfigBuilder.initializeIfNeeded(config)
        }
    }
    
    companion object Plugin : ApplicationPlugin<Application, Configuration, FlagshipPlugin> {
        override val key = io.ktor.util.AttributeKey<FlagshipPlugin>("FlagshipPlugin")
        
        override fun install(
            pipeline: Application,
            configure: Configuration.() -> Unit
        ): FlagshipPlugin {
            val configuration = Configuration().apply(configure)
            val plugin = FlagshipPlugin(configuration)
            
            // Install routing extension
            pipeline.routing {
                flagshipRoutes(plugin.flagsManager)
            }
            
            return plugin
        }
    }
}

/**
 * Extension function to easily install Flagship plugin.
 */
fun Application.flagship(config: FlagshipPlugin.Configuration.() -> Unit): FlagshipPlugin {
    return install(FlagshipPlugin, config)
}

