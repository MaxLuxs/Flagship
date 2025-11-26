package io.maxluxs.flagship.samplejs

import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.provider.rest.RestFlagsProvider
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

fun main() {
    initializeFlagship()
    renderApp()
}

private fun initializeFlagship() {
    val httpClient = HttpClient(Js) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    val baseUrl = window.location.origin + "/api/flags"
    
    val provider = RestFlagsProvider(
        client = httpClient,
        baseUrl = baseUrl
    )
    
    val config = FlagsConfig(
        appKey = "sample-js-app",
        environment = "production",
        providers = listOf(provider),
        cache = InMemoryCache(),
        logger = DefaultLogger()
    )
    
    Flagship.configure(config)
    
    MainScope().launch {
        try {
            Flagship.manager().ensureBootstrap(5000)
            updateUI()
        } catch (e: Exception) {
            console.error("Failed to bootstrap flags:", e)
            showError("Failed to load feature flags. Using defaults.")
            updateUI()
        }
    }
}

private fun renderApp() {
    val root = document.getElementById("root") ?: return
    
    root.innerHTML = """
        <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px;">
            <h1>🚩 Flagship JS Sample</h1>
            <p>This is a standalone JavaScript sample using Flagship API via REST.</p>
            
            <div id="status" style="padding: 10px; margin: 20px 0; border-radius: 4px; background: #f0f0f0;">
                Loading...
            </div>
            
            <div id="flags" style="margin-top: 20px;">
                <h2>Feature Flags</h2>
                <div id="flags-list"></div>
            </div>
            
            <div id="experiments" style="margin-top: 20px;">
                <h2>Experiments</h2>
                <div id="experiments-list"></div>
            </div>
            
            <div style="margin-top: 30px; padding: 15px; background: #e8f4f8; border-radius: 4px;">
                <h3>API Usage Example</h3>
                <pre style="background: white; padding: 10px; border-radius: 4px; overflow-x: auto;"><code>// Check if feature is enabled
if (Flagship.manager().isEnabled("new_feature")) {
    // Show new feature
}

// Get experiment assignment
val assignment = Flagship.manager().assign("experiment_name")
when (assignment?.variant) {
    "control" -> showControl()
    "variant_a" -> showVariantA()
}</code></pre>
            </div>
        </div>
    """.trimIndent()
}

private fun updateUI() {
    val statusDiv = document.getElementById("status")
    val flagsList = document.getElementById("flags-list")
    val experimentsList = document.getElementById("experiments-list")
    
    MainScope().launch {
        try {
            val manager = Flagship.manager()
            val allFlags = manager.listAllFlags()
            
            statusDiv?.innerHTML = """
                <strong>Status:</strong> ✅ Connected
                <br><strong>Flags loaded:</strong> ${allFlags.size}
            """.trimIndent()
            
            flagsList?.innerHTML = if (allFlags.isEmpty()) {
                "<p>No flags available. Make sure the REST API is running.</p>"
            } else {
                val flagsHtml = allFlags.entries.map { (key, value) ->
                    val enabled = manager.isEnabled(key, default = false)
                    """
                        <div style="padding: 8px; margin: 5px 0; background: white; border-left: 3px solid ${if (enabled) "#4caf50" else "#f44336"};">
                            <strong>$key</strong>: ${value.toString()} 
                            <span style="color: ${if (enabled) "#4caf50" else "#999"};">(${if (enabled) "enabled" else "disabled"})</span>
                        </div>
                    """.trimIndent()
                }.joinToString("<br>")
                flagsHtml
            }
            
            experimentsList?.innerHTML = """
                <p>Experiment assignments will appear here when experiments are configured.</p>
            """.trimIndent()
            
        } catch (e: Exception) {
            statusDiv?.innerHTML = """
                <strong>Status:</strong> ❌ Error: ${e.message}
            """.trimIndent()
        }
    }
}

private fun showError(message: String) {
    val statusDiv = document.getElementById("status")
    statusDiv?.innerHTML = """
        <strong>Status:</strong> ⚠️ $message
    """.trimIndent()
}

