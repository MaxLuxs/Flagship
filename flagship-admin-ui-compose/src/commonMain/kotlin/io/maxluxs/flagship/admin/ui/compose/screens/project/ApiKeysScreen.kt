package io.maxluxs.flagship.admin.ui.compose.screens.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.admin.ui.compose.api.AdminApiClient
import io.maxluxs.flagship.admin.ui.compose.util.ClipboardManager
import io.maxluxs.flagship.admin.ui.compose.util.createClipboardManager
import io.maxluxs.flagship.shared.api.ApiKeyResponse
import kotlinx.coroutines.launch
import io.maxluxs.flagship.ui.components.components.BrandedCard
import io.maxluxs.flagship.ui.components.components.BrandedButton
import io.maxluxs.flagship.ui.components.theme.BrandColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysScreen(
    apiBaseUrl: String,
    authToken: String,
    projectId: String,
    onNavigateBack: () -> Unit
) {
    var apiKeys by remember { mutableStateOf<List<ApiKeyResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createdApiKey by remember { mutableStateOf<ApiKeyResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val apiClient = remember { AdminApiClient(apiBaseUrl) }
    
    fun loadApiKeys() {
        scope.launch {
            isLoading = true
            try {
                apiKeys = apiClient.getApiKeys(authToken, projectId)
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Failed to load API keys: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(projectId) {
        loadApiKeys()
    }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create API Key")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (apiKeys.isEmpty()) {
                    item {
                        BrandedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No API keys yet. Create your first API key!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(apiKeys) { key ->
                        var isKeyVisible by remember { mutableStateOf(false) }
                        BrandedCard(
                            modifier = Modifier.fillMaxWidth()
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
                                        Text(
                                            text = key.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                        // Type badge
                                        Surface(
                                            color = when (key.type) {
                                                "ADMIN" -> BrandColors.FlagshipOrange.copy(alpha = 0.2f)
                                                else -> BrandColors.FlagshipGreen.copy(alpha = 0.2f)
                                            },
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = key.type,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = when (key.type) {
                                                    "ADMIN" -> BrandColors.FlagshipOrange
                                                    else -> BrandColors.FlagshipGreen
                                                }
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Type: ${key.type}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = "Key: ${if (isKeyVisible && key.key != "***") key.key else "••••••••••••••••"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                        IconButton(
                                            onClick = { isKeyVisible = !isKeyVisible },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (isKeyVisible) "Hide" else "Show",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                apiClient.deleteApiKey(authToken, projectId, key.id)
                                                apiKeys = apiKeys.filter { it.id != key.id }
                                                successMessage = "API key deleted successfully"
                                            } catch (e: Exception) {
                                                errorMessage = "Failed to delete API key: ${e.message}"
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
    
    if (showCreateDialog) {
        CreateApiKeyDialog(
            apiClient = apiClient,
            authToken = authToken,
            projectId = projectId,
            onDismiss = { showCreateDialog = false },
            onApiKeyCreated = { apiKey ->
                apiKeys = apiKeys + apiKey
                showCreateDialog = false
                createdApiKey = apiKey
            },
            onError = { errorMessage = it }
        )
    }
    
    createdApiKey?.let { apiKey ->
        ShowApiKeyDialog(
            apiKey = apiKey,
            onDismiss = { createdApiKey = null }
        )
    }
    
    // Show error message
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(5000)
            errorMessage = null
        }
        BrandedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
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

@Composable
fun CreateApiKeyDialog(
    apiClient: AdminApiClient,
    authToken: String,
    projectId: String,
    onDismiss: () -> Unit,
    onApiKeyCreated: (ApiKeyResponse) -> Unit,
    onError: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var keyType by remember { mutableStateOf("READ_ONLY") }
    var expirationDays by remember { mutableStateOf<String>("") }
    var isLoading by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    fun validateName(): Boolean {
        if (name.isBlank()) {
            nameError = "Name cannot be empty"
            return false
        }
        if (name.length > 100) {
            nameError = "Name cannot exceed 100 characters"
            return false
        }
        nameError = null
        return true
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create API Key") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        validateName()
                    },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } }
                )
                
                var expanded by remember { mutableStateOf(false) }
                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = keyType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("READ_ONLY", "ADMIN").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    keyType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = expirationDays,
                    onValueChange = { expirationDays = it },
                    label = { Text("Expiration (days, optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Leave empty for no expiration") }
                )
            }
        },
        confirmButton = {
            BrandedButton(
                onClick = {
                    if (validateName()) {
                        isLoading = true
                        scope.launch {
                            try {
                                val expirationDays = expirationDays.takeIf { it.isNotBlank() }?.toIntOrNull()
                                val apiKey = apiClient.createApiKey(authToken, projectId, name, keyType, expirationDays)
                                onApiKeyCreated(apiKey)
                            } catch (e: Exception) {
                                onError("Failed to create API key: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading && nameError == null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ShowApiKeyDialog(
    apiKey: ApiKeyResponse,
    onDismiss: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    val clipboardManager = createClipboardManager()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API Key Created") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⚠️ Save this key now! You won't be able to see it again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                
                OutlinedTextField(
                    value = apiKey.key,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                clipboardManager.copyToClipboard(apiKey.key)
                                copied = true
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy"
                            )
                        }
                    }
                )
                
                if (copied) {
                    Text(
                        text = "✓ Copied to clipboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            BrandedButton(onClick = onDismiss) {
                Text("I've saved it")
            }
        }
    )
}

