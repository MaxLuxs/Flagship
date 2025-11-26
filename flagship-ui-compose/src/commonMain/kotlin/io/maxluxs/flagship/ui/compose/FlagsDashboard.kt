package io.maxluxs.flagship.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.model.FlagKey
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.ui.components.components.BrandedTopBar

/**
 * Main dashboard for managing and debugging feature flags.
 *
 * Provides an interactive UI for:
 * - Viewing all active flags
 * - Managing local overrides for testing
 * - Viewing diagnostics and performance metrics
 * - Manually refreshing configuration
 *
 * @param manager The flags manager instance
 * @param modifier Optional modifier for the dashboard
 * @param allowOverrides Whether to allow setting local overrides (default: true)
 * @param allowEnvSwitch Whether to show environment switching (default: false)
 * @param showDiagnostics Whether to show diagnostics tab (default: true)
 * @param useDarkTheme Use dark theme (default: false)
 * @param colorScheme Custom color scheme (optional)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlagsDashboard(
    manager: FlagsManager,
    modifier: Modifier = Modifier,
    allowOverrides: Boolean = true,
    allowEnvSwitch: Boolean = false,
    showDiagnostics: Boolean = true,
    useDarkTheme: Boolean = false,
    colorScheme: ColorScheme? = null
) {
    var selectedTab by remember { mutableStateOf(0) }
    var overrides by remember { mutableStateOf<Map<FlagKey, FlagValue>>(emptyMap()) }

    LaunchedEffect(manager) {
        overrides = manager.listOverrides()
    }

    FlagshipTheme(useDarkTheme = useDarkTheme, colorScheme = colorScheme) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Branded top bar with logo
                BrandedTopBar(
                    title = "Flags Debug",
                    showLogo = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Modern PrimaryTabRow with custom indicator
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicator = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        ) {
                            val indicatorColor = MaterialTheme.colorScheme.primary
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = indicatorColor
                            ) {}
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Flags",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Overrides",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (overrides.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("${overrides.size}")
                                    }
                                }
                            }
                        }
                    )
                    if (showDiagnostics) {
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                Text(
                                    "Diagnostics",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        )
                    }
                    if (allowEnvSwitch) {
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = {
                                Text(
                                    "Settings",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> FlagsListScreen(manager, allowOverrides)
                    1 -> OverridesScreen(manager)
                    2 -> if (showDiagnostics) DiagnosticsScreen(manager)
                    3 -> if (allowEnvSwitch) SettingsScreen(manager)
                }
            }
        }
    }
}

