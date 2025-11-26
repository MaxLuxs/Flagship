package io.maxluxs.flagship.admin.ui.compose.screens.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.admin.ui.compose.api.AdminApiClient
import io.maxluxs.flagship.shared.api.ProviderHealthStatus
import io.maxluxs.flagship.shared.api.ProviderMetricsData
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ProvidersScreen(
    apiBaseUrl: String,
    authToken: String,
    projectId: String,
    onNavigateBack: () -> Unit
) {
    var healthStatus by remember { mutableStateOf<List<ProviderHealthStatus>>(emptyList()) }
    var selectedProvider by remember { mutableStateOf<String?>(null) }
    var providerMetrics by remember { mutableStateOf<List<ProviderMetricsData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val apiClient = remember { AdminApiClient(apiBaseUrl) }
    
    fun loadData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                healthStatus = apiClient.getProviderHealthStatus(authToken, projectId)
                if (selectedProvider != null) {
                    providerMetrics = apiClient.getProviderMetrics(authToken, projectId, selectedProvider)
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load provider analytics: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(projectId) {
        loadData()
    }
    
    LaunchedEffect(selectedProvider) {
        if (selectedProvider != null) {
            scope.launch {
                try {
                    providerMetrics = apiClient.getProviderMetrics(authToken, projectId, selectedProvider)
                } catch (e: Exception) {
                    errorMessage = "Failed to load provider metrics: ${e.message}"
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Provider Analytics",
                style = MaterialTheme.typography.headlineSmall
            )
            IconButton(onClick = { loadData() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        
        if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage!!,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        if (isLoading && healthStatus.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (healthStatus.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No provider metrics yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Provider metrics will appear here once SDK starts sending data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(healthStatus) { status ->
                    ProviderCard(
                        status = status,
                        isSelected = selectedProvider == status.providerName,
                        onClick = {
                            selectedProvider = if (selectedProvider == status.providerName) {
                                null
                            } else {
                                status.providerName
                            }
                        }
                    )
                }
                
                if (selectedProvider != null) {
                    item {
                        ProviderDetailsCard(
                            providerName = selectedProvider!!,
                            metrics = providerMetrics,
                            isLoading = isLoading && providerMetrics.isEmpty()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderCard(
    status: ProviderHealthStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val healthColor = when {
        status.isHealthy -> MaterialTheme.colorScheme.primary
        status.consecutiveFailures >= 5 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.errorContainer
    }
    
    val healthIcon = when {
        status.isHealthy -> "🟢"
        status.consecutiveFailures >= 5 -> "🔴"
        else -> "🟡"
    }
    
    val healthText = when {
        status.isHealthy -> "Healthy"
        status.consecutiveFailures >= 5 -> "Unhealthy"
        else -> "Degraded"
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = healthIcon,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Column {
                        Text(
                            text = status.providerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = healthText,
                            style = MaterialTheme.typography.bodySmall,
                            color = healthColor
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${(status.successRate * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = healthColor
                    )
                    Text(
                        text = "Success Rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Divider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Avg Response",
                    value = "${status.averageResponseTimeMs.toInt()}ms"
                )
                MetricItem(
                    label = "Failures",
                    value = "${status.consecutiveFailures}"
                )
                MetricItem(
                    label = "Last Success",
                    value = status.lastSuccessTimeMs?.let {
                        formatTimestamp(it)
                    } ?: "Never"
                )
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProviderDetailsCard(
    providerName: String,
    metrics: List<ProviderMetricsData>,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Metrics History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (metrics.isEmpty()) {
                Text(
                    text = "No metrics history available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                metrics.take(10).forEach { metric ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatTimestamp(metric.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Requests: ${metric.totalRequests}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${(metric.successRate * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${metric.averageResponseTimeMs.toInt()}ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (metric != metrics.last()) {
                        Divider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
fun formatTimestamp(timestamp: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val diff = now - timestamp
    
    return when {
        diff < 60_000L -> "${diff / 1000L}s ago"
        diff < 3600_000L -> "${diff / 60_000L}m ago"
        diff < 86400_000L -> "${diff / 3600_000L}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}

