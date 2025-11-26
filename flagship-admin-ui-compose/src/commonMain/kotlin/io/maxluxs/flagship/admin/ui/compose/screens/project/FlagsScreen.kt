package io.maxluxs.flagship.admin.ui.compose.screens.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Code
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.admin.ui.compose.api.AdminApiClient
import io.maxluxs.flagship.admin.ui.compose.util.retryWithBackoff
import io.maxluxs.flagship.shared.api.RestFlagValue
import io.maxluxs.flagship.shared.api.FlagResponse
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import io.maxluxs.flagship.admin.ui.compose.screens.project.CreateFlagDialog
import io.maxluxs.flagship.admin.ui.compose.screens.project.EditFlagDialog
import io.maxluxs.flagship.ui.components.components.BrandedCard
import io.maxluxs.flagship.ui.components.components.BrandedButton
import io.maxluxs.flagship.ui.components.theme.BrandColors
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlagsScreen(
    apiBaseUrl: String,
    authToken: String,
    projectId: String,
    onNavigateBack: () -> Unit
) {
    var flags by remember { mutableStateOf<List<FlagResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingFlagKey by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var sortOrder by remember { mutableStateOf("key") } // key, type, createdAt, updatedAt
    var isRefreshing by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val apiClient = remember { AdminApiClient(apiBaseUrl) }
    
    fun loadFlags(retry: Boolean = false) {
        scope.launch {
            isLoading = true
            isRefreshing = true
            try {
                flags = if (retry) {
                    retryWithBackoff(maxRetries = 3) {
                        apiClient.getFlagsDetailed(authToken, projectId)
                    }
                } else {
                    apiClient.getFlagsDetailed(authToken, projectId)
                }
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Failed to load flags: ${e.message}"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }
    
    LaunchedEffect(projectId) {
        loadFlags()
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Flag")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Search and filter bar
                BrandedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search flags") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {})
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedType ?: "All Types",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Filter by type") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Types") },
                                        onClick = {
                                            selectedType = null
                                            expanded = false
                                        }
                                    )
                                    listOf("bool", "string", "int", "double", "number", "json").forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                selectedType = type
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            var sortExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = sortExpanded,
                                onExpandedChange = { sortExpanded = !sortExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = when(sortOrder) {
                                        "key" -> "Sort by Key"
                                        "type" -> "Sort by Type"
                                        else -> "Sort"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Sort") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) }
                                )
                                ExposedDropdownMenu(
                                    expanded = sortExpanded,
                                    onDismissRequest = { sortExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Sort by Key") },
                                        onClick = {
                                            sortOrder = "key"
                                            sortExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sort by Type") },
                                        onClick = {
                                            sortOrder = "type"
                                            sortExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Filtered and sorted flags
                val filteredFlags = remember(flags, searchQuery, selectedType, sortOrder) {
                    flags
                        .filter { flag ->
                            val matchesSearch = searchQuery.isBlank() || flag.key.contains(searchQuery, ignoreCase = true)
                            val matchesType = selectedType == null || flag.type == selectedType
                            matchesSearch && matchesType
                        }
                        .sortedWith(compareBy { flag ->
                            when(sortOrder) {
                                "type" -> flag.type
                                else -> flag.key
                            }
                        })
                }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredFlags.isEmpty()) {
                        item {
                            BrandedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (flags.isEmpty()) "No flags yet. Create your first flag!" else "No flags match your search.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filteredFlags) { flag ->
                    BrandedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 2.dp,
                                color = if (flag.isEnabled) BrandColors.FlagshipGreen.copy(alpha = 0.3f) else Color.Transparent,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Flag type icon
                                    Icon(
                                        imageVector = when (flag.type) {
                                            "bool" -> Icons.Default.ToggleOn
                                            "string" -> Icons.Default.TextFields
                                            "int", "double", "number" -> Icons.Default.Numbers
                                            "json" -> Icons.Default.Code
                                            else -> Icons.Default.Code
                                        },
                                        contentDescription = flag.type,
                                        modifier = Modifier.size(20.dp),
                                        tint = when (flag.type) {
                                            "bool" -> BrandColors.FlagshipGreen
                                            "string" -> BrandColors.FlagshipOrange
                                            "int", "double", "number" -> MaterialTheme.colorScheme.primary
                                            "json" -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Text(
                                        text = flag.key,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    // Status indicator
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = if (flag.isEnabled) BrandColors.FlagshipGreen else BrandColors.TextTertiary,
                                                shape = androidx.compose.foundation.shape.CircleShape
                                            )
                                    )
                                }
                                Text(
                                    text = "${flag.type}: ${flag.value}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                flag.description?.let { desc ->
                                    if (desc.isNotBlank()) {
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = flag.isEnabled,
                                    onCheckedChange = { newValue ->
                                        scope.launch {
                                            try {
                                                val updatedFlag = apiClient.toggleFlag(authToken, projectId, flag.key, newValue)
                                                flags = flags.map { if (it.key == flag.key) updatedFlag else it }
                                                successMessage = "Flag ${if (newValue) "enabled" else "disabled"} successfully"
                                            } catch (e: Exception) {
                                                errorMessage = "Failed to toggle flag: ${e.message}"
                                            }
                                        }
                                    }
                                )
                                IconButton(
                                    onClick = { editingFlagKey = flag.key }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit"
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                apiClient.deleteFlag(authToken, projectId, flag.key)
                                                flags = flags.filter { it.key != flag.key }
                                                successMessage = "Flag deleted successfully"
                                            } catch (e: Exception) {
                                                errorMessage = "Failed to delete flag: ${e.message}"
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                        }
                    }
                }
            }
        }
        
        if (showCreateDialog) {
            CreateFlagDialog(
                apiClient = apiClient,
                authToken = authToken,
                projectId = projectId,
                existingKeys = flags.map { it.key }.toSet(),
                onDismiss = { showCreateDialog = false },
                onFlagCreated = { key: String, flag: RestFlagValue ->
                    // Reload flags to get full metadata
                    loadFlags()
                    showCreateDialog = false
                    successMessage = "Flag created successfully"
                },
                onError = { error: String -> errorMessage = error }
            )
        }
        
        editingFlagKey?.let { key: String ->
            flags.find { it.key == key }?.let { flag: FlagResponse ->
                // Convert FlagResponse to RestFlagValue for EditFlagDialog
                val restFlagValue = try {
                    when (flag.type) {
                        "bool" -> RestFlagValue(flag.type, kotlinx.serialization.json.JsonPrimitive(flag.value.toBoolean()))
                        "int" -> RestFlagValue(flag.type, kotlinx.serialization.json.JsonPrimitive(flag.value.toInt()))
                        "double", "number" -> RestFlagValue(flag.type, kotlinx.serialization.json.JsonPrimitive(flag.value.toDouble()))
                        "string" -> RestFlagValue(flag.type, kotlinx.serialization.json.JsonPrimitive(flag.value))
                        "json" -> RestFlagValue(flag.type, kotlinx.serialization.json.Json.parseToJsonElement(flag.value))
                        else -> RestFlagValue(flag.type, kotlinx.serialization.json.JsonPrimitive(flag.value))
                    }
                } catch (e: Exception) {
                    // Fallback to string if parsing fails
                    RestFlagValue(flag.type, kotlinx.serialization.json.JsonPrimitive(flag.value))
                }
                
                EditFlagDialog(
                    apiClient = apiClient,
                    authToken = authToken,
                    projectId = projectId,
                    flagKey = key,
                    currentFlag = restFlagValue,
                    onDismiss = { editingFlagKey = null },
                    onFlagUpdated = { updatedFlag: RestFlagValue ->
                        // Reload flags to get full metadata
                        loadFlags()
                        editingFlagKey = null
                        successMessage = "Flag updated successfully"
                    },
                    onError = { error: String -> errorMessage = error }
                )
            }
        }
    }
    
    // Show error message with retry
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(10000) // Longer delay if retry available
            errorMessage = null
        }
        BrandedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                if (message.contains("Tap to retry", ignoreCase = true) || message.contains("Failed to load", ignoreCase = true)) {
                    BrandedButton(
                        onClick = { loadFlags(retry = true) }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
    
    // Show success message
    successMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            successMessage = null
        }
        BrandedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = message,
                color = BrandColors.FlagshipGreen
            )
        }
    }
    }
}
