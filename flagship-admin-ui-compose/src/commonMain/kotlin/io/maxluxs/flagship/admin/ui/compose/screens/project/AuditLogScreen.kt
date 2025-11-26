package io.maxluxs.flagship.admin.ui.compose.screens.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.admin.ui.compose.api.AdminApiClient
import io.maxluxs.flagship.shared.api.AuditLogEntry
import io.maxluxs.flagship.shared.api.ErrorResponse
import io.maxluxs.flagship.ui.components.components.BrandedCard
import io.maxluxs.flagship.ui.components.components.BrandedButton
import io.maxluxs.flagship.ui.components.components.BrandedTopBar
import io.maxluxs.flagship.ui.components.theme.BrandColors
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(
    apiBaseUrl: String,
    authToken: String,
    projectId: String,
    onNavigateBack: () -> Unit
) {
    var logs by remember { mutableStateOf<List<AuditLogEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedActionType by remember { mutableStateOf<String?>(null) }
    var limit by remember { mutableStateOf(100) }
    var offset by remember { mutableStateOf(0) }
    
    val scope = rememberCoroutineScope()
    val apiClient = remember { AdminApiClient(apiBaseUrl) }
    
    fun loadLogs() {
        scope.launch {
            isLoading = true
            try {
                logs = apiClient.getAuditLogs(
                    authToken,
                    projectId,
                    limit = limit,
                    offset = offset,
                    actionType = selectedActionType
                )
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = try {
                    val errorResponse = Json.decodeFromString<ErrorResponse>(e.message ?: "")
                    errorResponse.details ?: errorResponse.error
                } catch (ex: Exception) {
                    "Failed to load audit logs: ${e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(projectId, selectedActionType, limit, offset) {
        loadLogs()
    }
    
    Scaffold(
        topBar = {
            BrandedTopBar(
                title = "Audit Log",
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
            // Filter by action type
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedActionType == null,
                        onClick = { selectedActionType = null },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("FLAG_CREATED", "FLAG_UPDATED", "FLAG_DELETED").forEach { action ->
                        FilterChip(
                            selected = selectedActionType == action,
                            onClick = { selectedActionType = action },
                            label = { Text(action.replace("_", " ")) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandColors.FlagshipGreenContainer,
                                selectedLabelColor = BrandColors.FlagshipGreen
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("EXPERIMENT_CREATED", "EXPERIMENT_UPDATED", "EXPERIMENT_DELETED").forEach { action ->
                        FilterChip(
                            selected = selectedActionType == action,
                            onClick = { selectedActionType = action },
                            label = { Text(action.replace("_", " ")) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandColors.FlagshipOrangeContainer,
                                selectedLabelColor = BrandColors.FlagshipOrange
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("PROJECT_CREATED", "PROJECT_UPDATED", "PROJECT_DELETED", "API_KEY_CREATED", "API_KEY_DELETED", "PROJECT_MEMBER_ADDED", "PROJECT_MEMBER_REMOVED").forEach { action ->
                        FilterChip(
                            selected = selectedActionType == action,
                            onClick = { selectedActionType = action },
                            label = { Text(action.replace("_", " ")) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
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
                            Icons.Default.Delete,
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
            } else if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No audit logs found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs) { log ->
                        AuditLogCard(log = log)
                    }
                    
                    // Load more button
                    if (logs.size >= limit) {
                        item {
                            BrandedButton(
                                onClick = {
                                    offset += limit
                                    loadLogs()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Load More")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogCard(log: AuditLogEntry) {
    // Цветовое кодирование типов действий
    val (actionColor, actionIcon, actionContainerColor) = when {
        log.action.contains("CREATED") -> Triple(
            BrandColors.FlagshipGreen,
            Icons.Default.PersonAdd,
            BrandColors.FlagshipGreenContainer
        )
        log.action.contains("UPDATED") -> Triple(
            BrandColors.FlagshipOrange,
            Icons.Default.Edit,
            BrandColors.FlagshipOrangeContainer
        )
        log.action.contains("DELETED") || log.action.contains("REMOVED") -> Triple(
            MaterialTheme.colorScheme.error,
            Icons.Default.Delete,
            MaterialTheme.colorScheme.errorContainer
        )
        log.action.contains("MEMBER") -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.Default.PersonAdd,
            MaterialTheme.colorScheme.primaryContainer
        )
        log.action.contains("API_KEY") -> Triple(
            MaterialTheme.colorScheme.secondary,
            Icons.Default.VpnKey,
            MaterialTheme.colorScheme.secondaryContainer
        )
        else -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.History,
            MaterialTheme.colorScheme.surfaceVariant
        )
    }
    
    BrandedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        actionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = actionColor
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = actionContainerColor
                    ) {
                        Text(
                            text = log.action.replace("_", " "),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = actionColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = formatDate(log.createdAt * 1000), // Convert seconds to milliseconds
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (log.entityType != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Entity:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${log.entityType}${log.entityId?.let { " - $it" } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            val userId = log.userId
            if (userId != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "User:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = userId,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            val changes = log.changes
            if (changes != null && changes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Changes:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                BrandedCard(
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        changes.forEach { (key, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = value.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            val ipAddress = log.ipAddress
            if (ipAddress != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IP:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = ipAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

