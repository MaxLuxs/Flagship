package io.maxluxs.flagship.ktor

import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.FlagsConfigBuilder

/**
 * Configuration for Flagship Ktor plugin.
 */
class FlagshipPluginConfiguration {
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
 *             val enabled = Flagship.isEnabled("new_api")
 *             // ...
 *         }
 *     }
 * }
 * ```
 */
val FlagshipPlugin = createApplicationPlugin(
    name = "FlagshipPlugin",
    createConfiguration = { FlagshipPluginConfiguration() }
) {
    val flagsManager = pluginConfig.build()
    
    // Install routing extension
    application.routing {
        flagshipRoutes(flagsManager)
    }
}

/**
 * Extension function to easily install Flagship plugin.
 */
fun Application.flagship(config: FlagshipPluginConfiguration.() -> Unit): io.ktor.server.application.PluginInstance {
    return install(FlagshipPlugin, config)
}

