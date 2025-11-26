package io.maxluxs.flagship.admin.ui.compose.navigation

/**
 * Navigation screens for the Admin Panel.
 */
sealed class Screen {
    data object Login : Screen()
    data object Register : Screen()
    data object Dashboard : Screen()
    data class ProjectDetail(val projectId: String) : Screen()
    data class FlagsManagement(val projectId: String) : Screen()
    data class ExperimentsManagement(val projectId: String) : Screen()
    data class ApiKeysManagement(val projectId: String) : Screen()
    data class ProjectMembers(val projectId: String) : Screen()
    data class Analytics(val projectId: String) : Screen()
    data class AuditLog(val projectId: String) : Screen()
    data object Settings : Screen()
}

