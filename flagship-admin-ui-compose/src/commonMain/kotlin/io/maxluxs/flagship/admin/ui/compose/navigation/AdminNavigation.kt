package io.maxluxs.flagship.admin.ui.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.maxluxs.flagship.admin.ui.compose.screens.dashboard.DashboardScreen
import io.maxluxs.flagship.admin.ui.compose.screens.login.LoginScreen
import io.maxluxs.flagship.admin.ui.compose.screens.login.RegisterScreen
import io.maxluxs.flagship.admin.ui.compose.screens.project.ApiKeysScreen
import io.maxluxs.flagship.admin.ui.compose.screens.project.AnalyticsScreen
import io.maxluxs.flagship.admin.ui.compose.screens.project.AuditLogScreen
import io.maxluxs.flagship.admin.ui.compose.screens.project.ExperimentsScreen
import io.maxluxs.flagship.admin.ui.compose.screens.project.FlagsScreen
import io.maxluxs.flagship.admin.ui.compose.screens.project.ProjectDetailScreen
import io.maxluxs.flagship.admin.ui.compose.screens.project.ProjectMembersScreen
import io.maxluxs.flagship.admin.ui.compose.screens.settings.ProfileSettingsScreen
import io.maxluxs.flagship.shared.api.UserResponse

/**
 * Navigation router for Admin Panel.
 */
@Composable
fun AdminNavigation(
    currentScreen: Screen,
    authToken: String?,
    currentUser: UserResponse?,
    apiBaseUrl: String,
    onNavigate: (Screen) -> Unit,
    onAuthTokenChange: (String?, UserResponse?) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        when (currentScreen) {
            is Screen.Login -> {
            LoginScreen(
                apiBaseUrl = apiBaseUrl,
                onLoginSuccess = { authResponse ->
                    onAuthTokenChange(authResponse.token, authResponse.user)
                    onNavigate(Screen.Dashboard)
                },
                onNavigateToRegister = {
                    onNavigate(Screen.Register)
                }
            )
        }
        
        is Screen.Register -> {
            RegisterScreen(
                apiBaseUrl = apiBaseUrl,
                onRegisterSuccess = { authResponse ->
                    onAuthTokenChange(authResponse.token, authResponse.user)
                    onNavigate(Screen.Dashboard)
                },
                onNavigateToLogin = {
                    onNavigate(Screen.Login)
                }
            )
        }
        
        is Screen.Dashboard -> {
            if (authToken != null) {
                DashboardScreen(
                    apiBaseUrl = apiBaseUrl,
                    authToken = authToken,
                    userEmail = currentUser?.email,
                    userName = currentUser?.name,
                    onProjectSelected = { projectId ->
                        onNavigate(Screen.ProjectDetail(projectId))
                    },
                    onNavigateToSettings = {
                        onNavigate(Screen.Settings)
                    },
                    onLogout = {
                        onAuthTokenChange(null, null)
                        onNavigate(Screen.Login)
                    }
                )
            } else {
                onNavigate(Screen.Login)
            }
        }
        
        is Screen.ProjectDetail -> {
            if (authToken != null) {
                ProjectDetailScreen(
                    apiBaseUrl = apiBaseUrl,
                    authToken = authToken,
                    projectId = currentScreen.projectId,
                    onNavigateToFlags = {
                        onNavigate(Screen.FlagsManagement(currentScreen.projectId))
                    },
                    onNavigateToExperiments = {
                        onNavigate(Screen.ExperimentsManagement(currentScreen.projectId))
                    },
                    onNavigateToApiKeys = {
                        onNavigate(Screen.ApiKeysManagement(currentScreen.projectId))
                    },
                    onNavigateToMembers = {
                        onNavigate(Screen.ProjectMembers(currentScreen.projectId))
                    },
                    onNavigateToAnalytics = {
                        onNavigate(Screen.Analytics(currentScreen.projectId))
                    },
                    onNavigateToAuditLog = {
                        onNavigate(Screen.AuditLog(currentScreen.projectId))
                    },
                    onNavigateBack = {
                        onNavigate(Screen.Dashboard)
                    }
                )
            } else {
                onNavigate(Screen.Login)
            }
        }
        
        is Screen.FlagsManagement -> {
            if (authToken != null) {
                FlagsScreen(
                    apiBaseUrl = apiBaseUrl,
                    authToken = authToken,
                    projectId = currentScreen.projectId,
                    onNavigateBack = {
                        onNavigate(Screen.ProjectDetail(currentScreen.projectId))
                    }
                )
            } else {
                onNavigate(Screen.Login)
            }
        }
        
        is Screen.ExperimentsManagement -> {
            if (authToken != null) {
                ExperimentsScreen(
                    apiBaseUrl = apiBaseUrl,
                    authToken = authToken,
                    projectId = currentScreen.projectId,
                    onNavigateBack = {
                        onNavigate(Screen.ProjectDetail(currentScreen.projectId))
                    }
                )
            } else {
                onNavigate(Screen.Login)
            }
        }
        
        is Screen.ApiKeysManagement -> {
            if (authToken != null) {
                ApiKeysScreen(
                    apiBaseUrl = apiBaseUrl,
                    authToken = authToken,
                    projectId = currentScreen.projectId,
                    onNavigateBack = {
                        onNavigate(Screen.ProjectDetail(currentScreen.projectId))
                    }
                )
            } else {
                onNavigate(Screen.Login)
            }
        }
        
        is Screen.ProjectMembers -> {
            if (authToken != null) {
                ProjectMembersScreen(
                    apiBaseUrl = apiBaseUrl,
                    authToken = authToken,
                    projectId = currentScreen.projectId,
                    onNavigateBack = {
                        onNavigate(Screen.ProjectDetail(currentScreen.projectId))
                    }
                )
            } else {
                onNavigate(Screen.Login)
            }
        }
        
        is Screen.Analytics -> {
            if (authToken != null) {
                AnalyticsScreen(
                    apiBaseUrl = apiBaseUrl,
                    authToken = authToken,
                    projectId = currentScreen.projectId,
                    onNavigateBack = {
                        onNavigate(Screen.ProjectDetail(currentScreen.projectId))
                    }
                )
            } else {
                onNavigate(Screen.Login)
            }
        }
        
        is Screen.AuditLog -> {
            if (authToken != null) {
                AuditLogScreen(
                    apiBaseUrl = apiBaseUrl,
                    authToken = authToken,
                    projectId = currentScreen.projectId,
                    onNavigateBack = {
                        onNavigate(Screen.ProjectDetail(currentScreen.projectId))
                    }
                )
            } else {
                onNavigate(Screen.Login)
            }
        }
        
        is Screen.Settings -> {
            if (authToken != null) {
                ProfileSettingsScreen(
                    apiBaseUrl = apiBaseUrl,
                    authToken = authToken,
                    currentUser = currentUser,
                    onNavigateBack = { onNavigate(Screen.Dashboard) },
                    onUserUpdated = { updatedUser ->
                        onAuthTokenChange(authToken, updatedUser)
                    }
                )
            } else {
                onNavigate(Screen.Login)
            }
        }
        }
    }
}

