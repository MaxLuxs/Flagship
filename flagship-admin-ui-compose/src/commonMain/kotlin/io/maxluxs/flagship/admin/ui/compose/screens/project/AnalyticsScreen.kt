package io.maxluxs.flagship.admin.ui.compose.screens.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.admin.ui.compose.api.AdminApiClient
import io.maxluxs.flagship.shared.api.*
import io.maxluxs.flagship.ui.components.components.BrandedCard
import io.maxluxs.flagship.ui.components.components.BrandedTopBar
import io.maxluxs.flagship.ui.components.theme.BrandColors
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    apiBaseUrl: String,
    authToken: String,
    projectId: String,
    onNavigateBack: () -> Unit
) {
    var overview by remember { mutableStateOf<AnalyticsOverview?>(null) }
    var providerMetrics by remember { mutableStateOf<List<ProviderMetricsData>>(emptyList()) }
    var providerHealth by remember { mutableStateOf<List<ProviderHealthStatus>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedPeriod by remember { mutableStateOf("24h") }
    
    val scope = rememberCoroutineScope()
    val apiClient = remember { AdminApiClient(apiBaseUrl) }
    
    fun loadAnalytics() {
        scope.launch {
            isLoading = true
            try {
                overview = apiClient.getAnalyticsOverview(authToken, projectId, selectedPeriod)
                providerHealth = apiClient.getProviderHealthStatus(authToken, projectId)
                providerMetrics = apiClient.getProviderMetrics(authToken, projectId)
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = try {
                    val errorResponse = Json.decodeFromString<ErrorResponse>(e.message ?: "")
                    errorResponse.details ?: errorResponse.error
                } catch (ex: Exception) {
                    "Failed to load analytics: ${e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(projectId, selectedPeriod) {
        loadAnalytics()
    }
    
    Scaffold(
        topBar = {
            BrandedTopBar(
                title = "Analytics",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Period selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("1h", "24h", "7d", "30d").forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Error message
            errorMessage?.let { error ->
                BrandedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Overview cards
                    overview?.let { overview ->
                        item {
                            Text(
                                text = "Overview",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OverviewCard(
                                    title = "Total Flags",
                                    value = overview.totalFlags.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                OverviewCard(
                                    title = "Total Experiments",
                                    value = overview.totalExperiments.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                OverviewCard(
                                    title = "Total Events",
                                    value = overview.totalEvents.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        // Flag stats
                        if (overview.flagStats.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Flag Statistics",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            items(overview.flagStats) { stat ->
                                FlagStatCard(stat = stat)
                            }
                        }
                        
                        // Experiment stats
                        if (overview.experimentStats.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Experiment Statistics",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            items(overview.experimentStats) { stat ->
                                ExperimentStatCard(stat = stat)
                            }
                        }
                    }
                    
                    // Provider health
                    if (providerHealth.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Provider Health",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(providerHealth) { health ->
                            ProviderHealthCard(health = health)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    BrandedCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FlagStatCard(stat: FlagStats) {
    BrandedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stat.flagKey,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (stat.enabledCount > stat.disabledCount) {
                        BrandColors.FlagshipGreenContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = if (stat.enabledCount > stat.disabledCount) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (stat.enabledCount > stat.disabledCount) {
                            BrandColors.FlagshipGreen
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stat.enabledCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandColors.FlagshipGreen
                    )
                    Text(
                        text = "Enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stat.disabledCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stat.totalRequests.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ExperimentStatCard(stat: ExperimentStats) {
    BrandedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = stat.experimentKey,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stat.totalAssignments.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Total Assignments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Variant Distribution",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            stat.variantDistribution.forEach { (variant, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = BrandColors.FlagshipGreenContainer,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Text(
                            text = variant,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ProviderHealthCard(health: ProviderHealthStatus) {
    val isHealthy = health.isHealthy
    val statusColor = if (isHealthy) BrandColors.FlagshipGreen else BrandColors.Error
    val statusContainerColor = if (isHealthy) BrandColors.FlagshipGreenContainer else MaterialTheme.colorScheme.errorContainer
    val statusIcon = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Error
    
    BrandedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    statusIcon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = statusColor
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = health.providerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${(health.successRate * 100).toInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                            Text(
                                text = "Success Rate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (health.consecutiveFailures > 0) {
                            Column {
                                Text(
                                    text = health.consecutiveFailures.toString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (health.consecutiveFailures > 3) BrandColors.Error else BrandColors.FlagshipOrange
                                )
                                Text(
                                    text = "Failures",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = statusContainerColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        statusIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = statusColor
                    )
                    Text(
                        text = if (isHealthy) "Healthy" else "Unhealthy",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }
        }
    }
}

