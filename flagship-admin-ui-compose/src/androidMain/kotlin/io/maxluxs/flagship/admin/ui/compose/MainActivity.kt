package io.maxluxs.flagship.admin.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Android entry point for Flagship Admin Panel.
 * 
 * This is the main activity that launches the admin UI on Android devices.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // Determine API URL based on device type
            // - Emulator: use 10.0.2.2 to access host machine's localhost
            // - Physical device: use actual IP address or configure via BuildConfig
            val apiBaseUrl = if (android.os.Build.FINGERPRINT.contains("generic")) {
                "http://10.0.2.2:8080" // Android emulator
            } else {
                // For physical devices, you may want to use BuildConfig or SharedPreferences
                // For now, default to localhost (works if device and server are on same network)
                "http://localhost:8080"
            }
            AdminApp(apiBaseUrl = apiBaseUrl)
        }
    }
}

@Preview
@Composable
fun AdminAppAndroidPreview() {
    AdminApp(apiBaseUrl = "http://localhost:8080")
}

