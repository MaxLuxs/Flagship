package io.maxluxs.flagship.admin.ui.compose

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.unit.dp

/**
 * Desktop entry point for Flagship Admin Panel.
 * 
 * Run this to launch the desktop admin application.
 * 
 * Usage:
 * - Run from IDE: Right-click on this file → Run 'MainKt'
 * - Run from command line: ./gradlew :flagship-admin-ui-compose:run
 * 
 * The application will connect to the Flagship server at http://localhost:8080 by default.
 * You can modify the apiBaseUrl parameter to point to a different server.
 */
fun main() = application {
    // Default API URL - can be configured via environment variable or system property
    val apiBaseUrl = System.getProperty("flagship.api.url") 
        ?: System.getenv("FLAGSHIP_API_URL") 
        ?: "http://localhost:8080"
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Flagship Admin",
        state = WindowState(width = 1200.dp, height = 800.dp)
    ) {
        AdminApp(apiBaseUrl = apiBaseUrl)
    }
}

