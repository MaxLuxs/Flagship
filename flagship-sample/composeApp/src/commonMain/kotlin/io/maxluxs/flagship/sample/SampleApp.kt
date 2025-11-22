package io.maxluxs.flagship.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.core.Flags
import io.maxluxs.flagship.ui.compose.FlagsDashboard
import io.maxluxs.flagship.ui.compose.FlagshipTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var flagsState by remember { mutableStateOf<FlagsState>(FlagsState.Loading) }
    var showProviderSelection by remember { mutableStateOf(false) }
    var currentProvider by remember { mutableStateOf(ProviderPreferences.getSelectedProvider()) }

    // Initialize flags manager
    LaunchedEffect(Unit) {
        try {
            val manager = Flags.manager()
            // Try to bootstrap with longer timeout, but allow fallback to cache
            val success = manager.ensureBootstrap(10000) // 10 seconds timeout
            
            if (success) {
                flagsState = FlagsState.Ready
            } else {
                // Bootstrap timed out, but check if we have cached values
                // If cache has values, we can still use the app
                val hasFlags = try {
                    manager.isEnabled("new_feature", default = false) || 
                    manager.listAllFlags().isNotEmpty()
                } catch (e: Exception) {
                    false
                }
                
                flagsState = if (hasFlags) {
                    // We have cached values, can proceed
                    FlagsState.Ready
                } else {
                    // No cache, show error
                    FlagsState.Error
                }
            }
        } catch (e: Exception) {
            println("Flags initialization error: ${e.message}")
            // Try to use cached values even if initialization failed
            try {
                val manager = Flags.manager()
                val hasFlags = manager.listAllFlags().isNotEmpty()
                flagsState = if (hasFlags) FlagsState.Ready else FlagsState.Error
            } catch (e2: Exception) {
                flagsState = FlagsState.Error
            }
        }
    }

    FlagshipTheme(useDarkTheme = false) {
        if (showProviderSelection || currentScreen == Screen.ProviderSelection) {
            ProviderSelectionScreen(
                currentProvider = currentProvider,
                onProviderSelected = { newProvider ->
                    ProviderPreferences.saveSelectedProvider(newProvider)
                    currentProvider = newProvider
                    showProviderSelection = false
                    // TODO: Reinitialize Flags with new provider
                    println("Provider switched to: ${newProvider.displayName}")
                },
                onContinue = {
                    showProviderSelection = false
                    currentScreen = Screen.Home
                }
            )
            return@FlagshipTheme
        }
        
        if (flagsState != FlagsState.Ready) {
            NotInitializedScreen(isLoading = flagsState == FlagsState.Loading)
            return@FlagshipTheme
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (currentScreen) {
                                Screen.Dashboard -> "Debug Dashboard"
                                Screen.ProviderSelection -> "Provider Selection"
                                else -> "Flagship Sample"
                            }
                        )
                    },
                    navigationIcon = {
                        if (currentScreen == Screen.Dashboard) {
                            IconButton(onClick = { currentScreen = Screen.Home }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        }
                    },
                    actions = {
                        if (currentScreen == Screen.Home) {
                            // Provider indicator
                            IconButton(onClick = { showProviderSelection = true }) {
                                Icon(
                                    when (currentProvider) {
                                        ProviderType.MOCK -> Icons.Default.Science
                                        ProviderType.REST -> Icons.Default.Api
                                        ProviderType.FIREBASE -> Icons.Default.Cloud
                                        ProviderType.LAUNCHDARKLY -> Icons.Default.Rocket
                                    },
                                    contentDescription = "Change Provider"
                                )
                            }
                            IconButton(onClick = { currentScreen = Screen.Dashboard }) {
                                Icon(Icons.Default.Settings, "Dashboard")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (currentScreen) {
                    Screen.Home -> HomeScreen(
                        currentProvider = currentProvider,
                        onNavigateToDashboard = { currentScreen = Screen.Dashboard }
                    )

                    Screen.Dashboard -> {
                        val manager = Flags.manager()
                        FlagsDashboard(
                            manager = manager,
                            allowOverrides = true,
                            allowEnvSwitch = false,
                            useDarkTheme = false
                        )
                    }
                    
                    Screen.ProviderSelection -> {
                        // Handled above
                    }
                }
            }
        }
    }
}

enum class FlagsState {
    Loading, Ready, Error
}

@Composable
fun HomeScreen(
    currentProvider: ProviderType,
    onNavigateToDashboard: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val manager = Flags.manager()

    // Feature flags - use state for reactive updates
    var newFeatureEnabled by remember { mutableStateOf(false) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    var paymentEnabled by remember { mutableStateOf(false) }

    // Config values
    var maxRetries by remember { mutableStateOf(3) }
    var apiTimeout by remember { mutableStateOf(30.0) }
    var welcomeMessage by remember { mutableStateOf("Welcome!") }

    // Experiments
    var testExperiment by remember { mutableStateOf<io.maxluxs.flagship.core.model.ExperimentAssignment?>(null) }
    var checkoutFlow by remember { mutableStateOf<io.maxluxs.flagship.core.model.ExperimentAssignment?>(null) }

    // Load initial data
    LaunchedEffect(manager) {
        newFeatureEnabled = manager.isEnabled("new_feature", default = false)
        darkModeEnabled = manager.isEnabled("dark_mode", default = false)
        paymentEnabled = manager.isEnabled("payment_enabled", default = false)
        maxRetries = manager.value("max_retries", 3)
        apiTimeout = manager.value("api_timeout", 30.0)
        welcomeMessage = manager.value("welcome_message", "Welcome!")
        testExperiment = manager.assign("test_experiment")
        checkoutFlow = manager.assign("checkout_flow")
    }

    // Listen for flag changes
    DisposableEffect(manager) {
        val listener = object : io.maxluxs.flagship.core.manager.FlagsListener {
            override fun onSnapshotUpdated(source: String) {
                scope.launch {
                    newFeatureEnabled = manager.isEnabled("new_feature", default = false)
                    darkModeEnabled = manager.isEnabled("dark_mode", default = false)
                    paymentEnabled = manager.isEnabled("payment_enabled", default = false)
                    maxRetries = manager.value("max_retries", 3)
                    apiTimeout = manager.value("api_timeout", 30.0)
                    welcomeMessage = manager.value("welcome_message", "Welcome!")
                    testExperiment = manager.assign("test_experiment")
                    checkoutFlow = manager.assign("checkout_flow")
                }
            }

            override fun onOverrideChanged(key: String) {
                scope.launch {
                    newFeatureEnabled = manager.isEnabled("new_feature", default = false)
                    darkModeEnabled = manager.isEnabled("dark_mode", default = false)
                    paymentEnabled = manager.isEnabled("payment_enabled", default = false)
                    maxRetries = manager.value("max_retries", 3)
                    apiTimeout = manager.value("api_timeout", 30.0)
                    welcomeMessage = manager.value("welcome_message", "Welcome!")
                    testExperiment = manager.assign("test_experiment")
                    checkoutFlow = manager.assign("checkout_flow")
                }
            }
        }
        manager.addListener(listener)
        onDispose {
            manager.removeListener(listener)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "🚀 Flagship Demo",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Feature flags & experiments showcase",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (currentProvider) {
                            ProviderType.MOCK -> Icons.Default.Science
                            ProviderType.REST -> Icons.Default.Api
                            ProviderType.FIREBASE -> Icons.Default.Cloud
                            ProviderType.LAUNCHDARKLY -> Icons.Default.Rocket
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Provider: ${currentProvider.displayName}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToDashboard,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Dashboard")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                manager.refresh()
                            } catch (e: Exception) {
                                println("Refresh failed: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh")
                }
            }
        }

        // Feature Flags Section
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Feature Flags",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            FeatureCard(
                title = "New Feature",
                icon = Icons.Default.NewReleases,
                flagKey = "new_feature",
                isEnabled = newFeatureEnabled,
                description = if (newFeatureEnabled)
                    "✅ New experimental feature is active"
                else
                    "Classic experience"
            )
        }

        item {
            FeatureCard(
                title = "Dark Mode",
                icon = Icons.Default.DarkMode,
                flagKey = "dark_mode",
                isEnabled = darkModeEnabled,
                description = if (darkModeEnabled)
                    "✅ Dark theme available"
                else
                    "Light theme only"
            )
        }

        item {
            FeatureCard(
                title = "Payment System",
                icon = Icons.Default.Payment,
                flagKey = "payment_enabled",
                isEnabled = paymentEnabled,
                description = if (paymentEnabled)
                    "✅ Payments are enabled"
                else
                    "⚠️ Payments temporarily disabled"
            )
        }

        // Experiments Section
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "A/B Tests",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            ExperimentCard(
                title = "Test Experiment",
                experimentKey = "test_experiment",
                variant = testExperiment?.variant ?: "none",
                description = when (testExperiment?.variant) {
                    "control" -> "Control group - standard experience"
                    "treatment" -> "Treatment group - new variant"
                    else -> "Not assigned to experiment"
                }
            )
        }

        item {
            ExperimentCard(
                title = "Checkout Flow",
                experimentKey = "checkout_flow",
                variant = checkoutFlow?.variant ?: "none",
                description = when (checkoutFlow?.variant) {
                    "control" -> "Standard checkout flow"
                    "variant_a" -> "Simplified checkout (variant A)"
                    "variant_b" -> "Express checkout (variant B)"
                    else -> "Not assigned to experiment"
                }
            )
        }

        // Configuration Section
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Remote Configuration",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            ConfigCard(
                title = "System Configuration",
                configs = listOf(
                    "Max Retries" to maxRetries.toString(),
                    "API Timeout" to "${apiTimeout}s",
                    "Welcome Message" to welcomeMessage
                )
            )
        }
    }
}

sealed class Screen {
    data object ProviderSelection : Screen()
    data object Home : Screen()
    data object Dashboard : Screen()
}
