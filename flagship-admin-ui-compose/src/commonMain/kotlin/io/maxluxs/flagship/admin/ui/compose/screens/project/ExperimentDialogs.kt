package io.maxluxs.flagship.admin.ui.compose.screens.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.admin.ui.compose.api.AdminApiClient
import io.maxluxs.flagship.shared.api.RestExperiment
import io.maxluxs.flagship.shared.api.RestVariant
import io.maxluxs.flagship.shared.api.RestTargeting
import kotlinx.coroutines.launch
import io.maxluxs.flagship.ui.components.components.BrandedButton
import io.maxluxs.flagship.ui.components.components.BrandedCard
import io.maxluxs.flagship.ui.components.theme.BrandColors

@Composable
fun CreateExperimentDialog(
    apiClient: AdminApiClient,
    authToken: String,
    projectId: String,
    existingKeys: Set<String>,
    onDismiss: () -> Unit,
    onExperimentCreated: (String, RestExperiment) -> Unit,
    onError: (String) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var variants by remember { mutableStateOf(listOf(RestVariant("control", 0.5, emptyMap()), RestVariant("variant", 0.5, emptyMap()))) }
    var exposureType by remember { mutableStateOf("onAssign") }
    var targeting by remember { mutableStateOf<RestTargeting?>(null) }
    var showTargeting by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var keyError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    fun validateKey(): Boolean {
        if (key.isBlank()) {
            keyError = "Key cannot be empty"
            return false
        }
        if (!key.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            keyError = "Key can only contain letters, numbers, underscores, and hyphens"
            return false
        }
        if (key.length > 100) {
            keyError = "Key cannot exceed 100 characters"
            return false
        }
        if (existingKeys.contains(key)) {
            keyError = "Experiment with this key already exists"
            return false
        }
        keyError = null
        return true
    }
    
    fun validateWeights(): Boolean {
        val totalWeight = variants.sumOf { it.weight.toDouble() }
        if (kotlin.math.abs(totalWeight - 1.0) > 0.001) {
            weightError = "Total weight must equal 1.0 (current: ${(totalWeight * 100).toInt() / 100.0})"
            return false
        }
        if (variants.any { it.weight < 0.0 || it.weight > 1.0 }) {
            weightError = "Each weight must be between 0.0 and 1.0"
            return false
        }
        if (variants.map { it.name }.distinct().size != variants.size) {
            weightError = "Variant names must be unique"
            return false
        }
        weightError = null
        return true
    }
    
    fun normalizeWeights() {
        val total = variants.sumOf { it.weight.toDouble() }
        if (total > 0) {
            variants = variants.map { 
                RestVariant(it.name, (it.weight / total).toDouble(), it.payload)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Experiment") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { 
                        key = it
                        validateKey()
                    },
                    label = { Text("Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = keyError != null,
                    supportingText = keyError?.let { { Text(it) } }
                )
                
                Text("Variants:", style = MaterialTheme.typography.titleSmall)
                
                variants.forEachIndexed { index, variant ->
                    BrandedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = variant.name,
                                    onValueChange = { newName ->
                                        variants = variants.toMutableList().apply {
                                            this[index] = RestVariant(newName, variant.weight, variant.payload)
                                        }
                                        validateWeights()
                                    },
                                    label = { Text("Name") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                
                                if (variants.size > 1) {
                                    IconButton(
                                        onClick = {
                                            variants = variants.toMutableList().apply { removeAt(index) }
                                            normalizeWeights()
                                            validateWeights()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = variant.weight.toString(),
                                    onValueChange = { weightStr ->
                                        val weight = weightStr.toDoubleOrNull() ?: 0.0
                                        variants = variants.toMutableList().apply {
                                            this[index] = RestVariant(variant.name, weight, variant.payload)
                                        }
                                        normalizeWeights()
                                        validateWeights()
                                    },
                                    label = { Text("Weight") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Text(
                                    text = "${(variant.weight * 100).toInt()}%",
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                
                BrandedButton(
                    onClick = {
                        variants = variants + RestVariant("variant_${variants.size + 1}", 0.0, emptyMap())
                        normalizeWeights()
                        validateWeights()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Variant")
                }
                
                if (weightError != null) {
                    Text(
                        text = weightError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                var expanded by remember { mutableStateOf(false) }
                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = exposureType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Exposure Type") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("onAssign", "onImpression").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    exposureType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            BrandedButton(
                onClick = {
                    if (validateKey() && validateWeights()) {
                        isLoading = true
                        scope.launch {
                            try {
                                val experiment = RestExperiment(
                                    variants = variants,
                                    targeting = targeting,
                                    exposureType = exposureType
                                )
                                apiClient.createExperiment(authToken, projectId, key, experiment)
                                onExperimentCreated(key, experiment)
                            } catch (e: Exception) {
                                onError("Failed to create experiment: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading && keyError == null && weightError == null
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
fun EditExperimentDialog(
    apiClient: AdminApiClient,
    authToken: String,
    projectId: String,
    experimentKey: String,
    currentExperiment: RestExperiment,
    onDismiss: () -> Unit,
    onExperimentUpdated: (RestExperiment) -> Unit,
    onError: (String) -> Unit
) {
    var variants by remember { mutableStateOf(currentExperiment.variants) }
    var exposureType by remember { mutableStateOf(currentExperiment.exposureType) }
    var targeting by remember { mutableStateOf(currentExperiment.targeting) }
    var showTargeting by remember { mutableStateOf(currentExperiment.targeting != null) }
    var isLoading by remember { mutableStateOf(false) }
    var weightError by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    fun validateWeights(): Boolean {
        val totalWeight = variants.sumOf { it.weight.toDouble() }
        if (kotlin.math.abs(totalWeight - 1.0) > 0.001) {
            weightError = "Total weight must equal 1.0 (current: ${(totalWeight * 100).toInt() / 100.0})"
            return false
        }
        if (variants.any { it.weight < 0.0 || it.weight > 1.0 }) {
            weightError = "Each weight must be between 0.0 and 1.0"
            return false
        }
        if (variants.map { it.name }.distinct().size != variants.size) {
            weightError = "Variant names must be unique"
            return false
        }
        weightError = null
        return true
    }
    
    fun normalizeWeights() {
        val total = variants.sumOf { it.weight.toDouble() }
        if (total > 0) {
            variants = variants.map { 
                RestVariant(it.name, (it.weight / total).toDouble(), it.payload)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Experiment: $experimentKey") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Variants:", style = MaterialTheme.typography.titleSmall)
                
                variants.forEachIndexed { index, variant ->
                    BrandedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = variant.name,
                                    onValueChange = { newName ->
                                        variants = variants.toMutableList().apply {
                                            this[index] = RestVariant(newName, variant.weight, variant.payload)
                                        }
                                        validateWeights()
                                    },
                                    label = { Text("Name") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                
                                if (variants.size > 1) {
                                    IconButton(
                                        onClick = {
                                            variants = variants.toMutableList().apply { removeAt(index) }
                                            normalizeWeights()
                                            validateWeights()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = variant.weight.toString(),
                                    onValueChange = { weightStr ->
                                        val weight = weightStr.toDoubleOrNull() ?: 0.0
                                        variants = variants.toMutableList().apply {
                                            this[index] = RestVariant(variant.name, weight, variant.payload)
                                        }
                                        normalizeWeights()
                                        validateWeights()
                                    },
                                    label = { Text("Weight") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Text(
                                    text = "${(variant.weight * 100).toInt()}%",
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                
                BrandedButton(
                    onClick = {
                        variants = variants + RestVariant("variant_${variants.size + 1}", 0.0, emptyMap())
                        normalizeWeights()
                        validateWeights()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Variant")
                }
                
                if (weightError != null) {
                    Text(
                        text = weightError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                var expanded by remember { mutableStateOf(false) }
                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = exposureType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Exposure Type") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("onAssign", "onImpression").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    exposureType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Targeting section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Targeting (optional)")
                    Switch(
                        checked = showTargeting,
                        onCheckedChange = { 
                            showTargeting = it
                            if (!it) targeting = null
                        }
                    )
                }
                
                if (showTargeting) {
                    SimpleTargetingEditor(
                        targeting = targeting,
                        onTargetingChange = { targeting = it }
                    )
                }
            }
        },
        confirmButton = {
            BrandedButton(
                onClick = {
                    if (validateWeights()) {
                        isLoading = true
                        scope.launch {
                            try {
                                val experiment = RestExperiment(
                                    variants = variants,
                                    targeting = targeting,
                                    exposureType = exposureType
                                )
                                apiClient.updateExperiment(authToken, projectId, experimentKey, experiment)
                                onExperimentUpdated(experiment)
                            } catch (e: Exception) {
                                onError("Failed to update experiment: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading && weightError == null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Update")
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

