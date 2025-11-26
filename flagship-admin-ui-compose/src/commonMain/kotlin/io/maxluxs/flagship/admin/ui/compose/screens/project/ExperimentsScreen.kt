package io.maxluxs.flagship.admin.ui.compose.screens.project

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.admin.ui.compose.api.AdminApiClient
import io.maxluxs.flagship.admin.ui.compose.util.retryWithBackoff
import io.maxluxs.flagship.shared.api.RestExperiment
import io.maxluxs.flagship.shared.api.RestVariant
import kotlinx.coroutines.launch
import io.maxluxs.flagship.ui.components.components.BrandedCard
import io.maxluxs.flagship.ui.components.components.BrandedButton
import io.maxluxs.flagship.ui.components.theme.BrandColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentsScreen(
    apiBaseUrl: String,
    authToken: String,
    projectId: String,
    onNavigateBack: () -> Unit
) {
    var experiments by remember { mutableStateOf<Map<String, RestExperiment>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingExperimentKey by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val apiClient = remember { AdminApiClient(apiBaseUrl) }
    
    fun loadExperiments(retry: Boolean = false) {
        scope.launch {
            isLoading = true
            try {
                experiments = if (retry) {
                    retryWithBackoff(maxRetries = 3) {
                        apiClient.getExperiments(authToken, projectId)
                    }
                } else {
                    apiClient.getExperiments(authToken, projectId)
                }
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Failed to load experiments: ${e.message}. Tap to retry."
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(projectId) {
        loadExperiments()
    }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Experiment")
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
                // Search bar
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
                            label = { Text("Search experiments") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {})
                        )
                        
                        // Refresh button
                        BrandedButton(
                            onClick = { loadExperiments(retry = true) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Text("Refresh")
                            }
                        }
                    }
                }
                
                // Filtered experiments
                val filteredExperiments = remember(experiments, searchQuery) {
                    experiments.entries
                        .filter { (key, _) ->
                            searchQuery.isBlank() || key.contains(searchQuery, ignoreCase = true)
                        }
                        .sortedBy { it.key }
                }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredExperiments.isEmpty()) {
                        item {
                            BrandedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (experiments.isEmpty()) "No experiments yet. Create your first experiment!" else "No experiments match your search.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filteredExperiments) { (key, experiment) ->
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
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    // Variant distribution visualization
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            experiment.variants.forEachIndexed { index, variant ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(variant.weight.toFloat())
                                                        .height(12.dp)
                                                        .background(
                                                            when (index % 3) {
                                                                0 -> BrandColors.FlagshipGreen.copy(alpha = 0.6f + variant.weight.toFloat() * 0.4f)
                                                                1 -> BrandColors.FlagshipOrange.copy(alpha = 0.6f + variant.weight.toFloat() * 0.4f)
                                                                else -> BrandColors.FlagshipGreen.copy(alpha = 0.4f + variant.weight.toFloat() * 0.3f)
                                                            }
                                                        )
                                                )
                                            }
                                        }
                                        Text(
                                            text = experiment.variants.joinToString { "${it.name} (${(it.weight * 100).toInt()}%)" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                    Text(
                                        text = "Exposure: ${experiment.exposureType}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = { editingExperimentKey = key }
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
                                                    apiClient.deleteExperiment(authToken, projectId, key)
                                                    experiments = experiments - key
                                                    successMessage = "Experiment deleted successfully"
                                                } catch (e: Exception) {
                                                    errorMessage = "Failed to delete experiment: ${e.message}"
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
    }
    
    if (showCreateDialog) {
        CreateExperimentDialog(
            apiClient = apiClient,
            authToken = authToken,
            projectId = projectId,
            existingKeys = experiments.keys.toSet(),
            onDismiss = { showCreateDialog = false },
            onExperimentCreated = { key, experiment ->
                experiments = experiments + (key to experiment)
                showCreateDialog = false
                successMessage = "Experiment created successfully"
            },
            onError = { errorMessage = it }
        )
    }
    
    editingExperimentKey?.let { key ->
        experiments[key]?.let { experiment ->
            EditExperimentDialog(
                apiClient = apiClient,
                authToken = authToken,
                projectId = projectId,
                experimentKey = key,
                currentExperiment = experiment,
                onDismiss = { editingExperimentKey = null },
                onExperimentUpdated = { updatedExperiment ->
                    experiments = experiments + (key to updatedExperiment)
                    editingExperimentKey = null
                    successMessage = "Experiment updated successfully"
                },
                onError = { errorMessage = it }
            )
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
                if (message.contains("Tap to retry", ignoreCase = true) || message.contains("Failed to load", ignoreCase = true)) {
                    BrandedButton(
                        onClick = { loadExperiments(retry = true) }
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

