package io.maxluxs.flagship.admin.ui.compose

import org.jetbrains.compose.web.renderComposable
import kotlinx.browser.window

/**
 * Web (JS) entry point for Flagship Admin Panel.
 * 
 * This is the main function that launches the admin UI in a web browser.
 * 
 * Usage:
 * - Build JS: ./gradlew :flagship-admin-ui-compose:jsBrowserDevelopmentWebpack
 * - Run server: ./gradlew :flagship-server:run
 * - Open in browser: http://localhost:8080/admin/
 * 
 * The application automatically determines the API URL from window.location.origin,
 * so it works with any server configuration.
 */
fun main() {
    // Automatically determine API URL from current window location
    // This allows the app to work with any server URL (localhost, production, etc.)
    val apiBaseUrl = window.location.origin
    
    renderComposable(rootElementId = "root") {
        AdminApp(apiBaseUrl = apiBaseUrl)
    }
}

