package io.maxluxs.flagship.admin.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.maxluxs.flagship.admin.ui.compose.navigation.AdminNavigation
import io.maxluxs.flagship.admin.ui.compose.navigation.AdaptiveNavigationScaffold
import io.maxluxs.flagship.admin.ui.compose.navigation.Screen
import io.maxluxs.flagship.admin.ui.compose.theme.AdminTheme
import io.maxluxs.flagship.shared.api.UserResponse

/**
 * Main entry point for the Admin Panel UI.
 *
 * This is a Compose Multiplatform application that can run on:
 * - Android
 * - iOS
 * - Desktop (JVM)
 * - Web (JS)
 *
 * Использует адаптивную навигацию:
 * - Desktop/Web (> 840dp): Sidebar навигация
 * - Tablet (600-840dp): Drawer навигация
 * - Mobile (< 600dp): Bottom navigation
 *
 * @param apiBaseUrl Base URL for the Flagship server API (default: "http://localhost:8080")
 * @param modifier Optional modifier
 */
@Composable
fun AdminApp(
    apiBaseUrl: String = "http://localhost:8080",
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
    var authToken by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<UserResponse?>(null) }

    AdminTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AdaptiveNavigationScaffold(
                currentScreen = currentScreen,
                authToken = authToken,
                currentUser = currentUser,
                onNavigate = { screen -> currentScreen = screen }
            ) { padding ->
                AdminNavigation(
                    currentScreen = currentScreen,
                    authToken = authToken,
                    currentUser = currentUser,
                    apiBaseUrl = apiBaseUrl,
                    onNavigate = { screen -> currentScreen = screen },
                    onAuthTokenChange = { token, user ->
                        authToken = token
                        currentUser = user
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

