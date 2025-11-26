package io.maxluxs.flagship.admin.ui.compose.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.ui.components.components.BrandedLogo
import io.maxluxs.flagship.admin.ui.compose.util.WindowSize
import io.maxluxs.flagship.admin.ui.compose.util.rememberWindowSize
import io.maxluxs.flagship.shared.api.UserResponse

/**
 * Adaptive Scaffold with navigation that changes based on screen size.
 * 
 * - Expanded (desktop/web > 840dp): Sidebar navigation on the left
 * - Medium (tablet 600-840dp): Drawer navigation
 * - Compact (mobile < 600dp): Bottom navigation bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveNavigationScaffold(
    currentScreen: Screen,
    authToken: String?,
    currentUser: UserResponse?,
    windowSize: WindowSize = rememberWindowSize(),
    onNavigate: (Screen) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val showNavigation = authToken != null && currentScreen !is Screen.Login && currentScreen !is Screen.Register
    
    when {
        windowSize.isExpanded -> {
            // Desktop/Web: Sidebar navigation
            Row(modifier = Modifier.fillMaxSize()) {
                if (showNavigation) {
                    // Custom sidebar
                    Column(
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        BrandedLogo(modifier = Modifier.padding(bottom = 32.dp))
                        SidebarNavigationItem(
                            selected = currentScreen is Screen.Dashboard,
                            onClick = { onNavigate(Screen.Dashboard) },
                            icon = Icons.Default.Dashboard,
                            label = "Dashboard"
                        )
                        SidebarNavigationItem(
                            selected = currentScreen is Screen.Settings,
                            onClick = { onNavigate(Screen.Settings) },
                            icon = Icons.Default.Settings,
                            label = "Settings"
                        )
                    }
                    VerticalDivider()
                }
                Box(modifier = Modifier.weight(1f)) {
                    content(PaddingValues(0.dp))
                }
            }
        }
        windowSize.isMedium -> {
            // Tablet: Drawer navigation
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(16.dp)
                        ) {
                            BrandedLogo(modifier = Modifier.padding(bottom = 24.dp))
                            NavigationDrawerItem(
                                selected = currentScreen is Screen.Dashboard,
                                onClick = {
                                    onNavigate(Screen.Dashboard)
                                    scope.launch {
                                        drawerState.close()
                                    }
                                },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                label = { Text("Dashboard") }
                            )
                            NavigationDrawerItem(
                                selected = currentScreen is Screen.Settings,
                                onClick = {
                                    onNavigate(Screen.Settings)
                                    scope.launch {
                                        drawerState.close()
                                    }
                                },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Settings") }
                            )
                        }
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Flagship Admin") },
                            navigationIcon = {
                                IconButton(onClick = {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            }
                        )
                    }
                ) { padding ->
                    content(padding)
                }
            }
        }
        else -> {
            // Mobile: Bottom navigation (simplified version)
            Scaffold(
                bottomBar = {
                    if (showNavigation) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                BottomNavItem(
                                    selected = currentScreen is Screen.Dashboard,
                                    onClick = { onNavigate(Screen.Dashboard) },
                                    icon = Icons.Default.Dashboard,
                                    label = "Dashboard"
                                )
                                BottomNavItem(
                                    selected = currentScreen is Screen.Settings,
                                    onClick = { onNavigate(Screen.Settings) },
                                    icon = Icons.Default.Settings,
                                    label = "Settings"
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                content(padding)
            }
        }
    }
}

/**
 * Navigation item for sidebar (desktop).
 */
@Composable
private fun SidebarNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Navigation item for bottom bar (mobile).
 */
@Composable
private fun BottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Determines the index of the selected navigation item.
 */
private fun getSelectedNavigationItem(screen: Screen): Int {
    return when (screen) {
        is Screen.Dashboard -> 0
        is Screen.Settings -> 1
        else -> -1
    }
}

/**
 * Gets screen by navigation index.
 */
private fun getNavigationItemByIndex(index: Int): Screen {
    return when (index) {
        0 -> Screen.Dashboard
        1 -> Screen.Settings
        else -> Screen.Dashboard
    }
}

