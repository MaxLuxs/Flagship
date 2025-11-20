package io.maxluxs.flagship.ui.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.core.manager.FlagsListener
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.model.FlagKey
import io.maxluxs.flagship.core.model.FlagValue

enum class FlagTypeFilter {
    ALL, BOOL, INT, DOUBLE, STRING, JSON
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlagsListScreen(
    manager: FlagsManager,
    allowOverrides: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(FlagTypeFilter.ALL) }
    var allFlags by remember { mutableStateOf(manager.listAllFlags()) }
    var overrides by remember { mutableStateOf(manager.listOverrides()) }
    var updateTrigger by remember { mutableStateOf(0) }
    
    // Listen for changes
    DisposableEffect(manager) {
        val listener = object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {
                allFlags = manager.listAllFlags()
                overrides = manager.listOverrides()
                updateTrigger++
            }
            override fun onOverrideChanged(key: FlagKey) {
                allFlags = manager.listAllFlags()
                overrides = manager.listOverrides()
                updateTrigger++
            }
        }
        manager.addListener(listener)
        onDispose {
            manager.removeListener(listener)
        }
    }

    // Calculate stats
    val stats = remember(allFlags, overrides, updateTrigger) {
        val boolCount = allFlags.count { it.value is FlagValue.Bool }
        val intCount = allFlags.count { it.value is FlagValue.Int }
        val doubleCount = allFlags.count { it.value is FlagValue.Double }
        val stringCount = allFlags.count { it.value is FlagValue.StringV }
        val jsonCount = allFlags.count { it.value is FlagValue.Json }
        mapOf(
            FlagTypeFilter.ALL to allFlags.size,
            FlagTypeFilter.BOOL to boolCount,
            FlagTypeFilter.INT to intCount,
            FlagTypeFilter.DOUBLE to doubleCount,
            FlagTypeFilter.STRING to stringCount,
            FlagTypeFilter.JSON to jsonCount
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search + Stats header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search flags") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            // Filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(FlagTypeFilter.entries) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { 
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(filter.name)
                                if (stats[filter] != 0) {
                                    Text(
                                        "(${stats[filter]})",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = if (selectedFilter == filter) {
                            FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = true,
                                borderColor = MaterialTheme.colorScheme.primary,
                                borderWidth = 2.dp
                            )
                        } else {
                            FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false
                            )
                        }
                    )
                }
            }
        }

        if (allFlags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📭",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = "No flags loaded yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Make sure flags are configured properly",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val filteredKeys = allFlags.keys.toList().filter { flagKey ->
                    val matchesSearch = searchQuery.isEmpty() || 
                        flagKey.contains(searchQuery, ignoreCase = true)
                    val matchesFilter = selectedFilter == FlagTypeFilter.ALL ||
                        when (selectedFilter) {
                            FlagTypeFilter.BOOL -> allFlags[flagKey] is FlagValue.Bool
                            FlagTypeFilter.INT -> allFlags[flagKey] is FlagValue.Int
                            FlagTypeFilter.DOUBLE -> allFlags[flagKey] is FlagValue.Double
                            FlagTypeFilter.STRING -> allFlags[flagKey] is FlagValue.StringV
                            FlagTypeFilter.JSON -> allFlags[flagKey] is FlagValue.Json
                            else -> true
                        }
                    matchesSearch && matchesFilter
                }
                
                items(
                    items = filteredKeys,
                    key = { flagKey -> "$flagKey-$updateTrigger" }
                ) { flagKey ->
                    val evaluatedValue = overrides[flagKey] ?: allFlags[flagKey]
                    
                    FlagItem(
                        key = flagKey,
                        value = evaluatedValue,
                        hasOverride = overrides.containsKey(flagKey),
                        manager = manager,
                        allowOverrides = allowOverrides
                    )
                }
                
                if (filteredKeys.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔍 No flags match your filters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlagItem(
    key: FlagKey,
    value: FlagValue?,
    hasOverride: Boolean,
    manager: FlagsManager,
    allowOverrides: Boolean
) {
    var currentValue by remember(key) { mutableStateOf(value) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(value) {
        currentValue = value
    }
    
    // Animated container color
    val containerColor by animateColorAsState(
        targetValue = if (hasOverride) 
            MaterialTheme.colorScheme.primaryContainer
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300)
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (hasOverride) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with key and badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = getValueTypeLabel(currentValue),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (hasOverride) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            "OVERRIDE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Value display with switch/control
            when (val flagValue = currentValue) {
                is FlagValue.Bool -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (flagValue.value) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (flagValue.value) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (allowOverrides) {
                            Switch(
                                checked = flagValue.value,
                                onCheckedChange = { checked ->
                                    currentValue = FlagValue.Bool(checked)
                                    manager.setOverride(key, FlagValue.Bool(checked))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        } else {
                            Surface(
                                color = if (flagValue.value)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    if (flagValue.value) "ON" else "OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (flagValue.value)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
                is FlagValue.Int, is FlagValue.Double, is FlagValue.StringV, is FlagValue.Json -> {
                    Surface(
                        onClick = { if (allowOverrides) showEditDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        enabled = allowOverrides
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (flagValue) {
                                    is FlagValue.Int -> flagValue.value.toString()
                                    is FlagValue.Double -> flagValue.value.toString()
                                    is FlagValue.StringV -> flagValue.value
                                    is FlagValue.Json -> flagValue.value.toString()
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (allowOverrides) {
                                Text(
                                    text = "✎",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                null -> {
                    Text(
                        text = "N/A",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Clear override button
            if (allowOverrides && hasOverride) {
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = {
                        manager.clearOverride(key)
                        // Value will be updated by the listener callback
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("✕ Clear Override")
                }
            }
        }
    }
    
    // Edit dialog for non-boolean values
    if (showEditDialog && currentValue != null) {
        EditValueDialog(
            key = key,
            currentValue = currentValue!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { newValue ->
                currentValue = newValue
                manager.setOverride(key, newValue)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun EditValueDialog(
    key: FlagKey,
    currentValue: FlagValue,
    onDismiss: () -> Unit,
    onConfirm: (FlagValue) -> Unit
) {
    var textValue by remember { 
        mutableStateOf(
            when (currentValue) {
                is FlagValue.Int -> currentValue.value.toString()
                is FlagValue.Double -> currentValue.value.toString()
                is FlagValue.StringV -> currentValue.value
                else -> ""
            }
        )
    }
    var isError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Override") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Key: $key",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Type: ${getValueTypeLabel(currentValue)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { 
                        textValue = it
                        isError = when (currentValue) {
                            is FlagValue.Int -> it.toIntOrNull() == null
                            is FlagValue.Double -> it.toDoubleOrNull() == null
                            else -> false
                        }
                    },
                    label = { Text("Value") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Invalid ${getValueTypeLabel(currentValue).lowercase()} format") }
                    } else null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newValue = when (currentValue) {
                        is FlagValue.Int -> {
                            textValue.toIntOrNull()?.let { FlagValue.Int(it) }
                        }
                        is FlagValue.Double -> {
                            textValue.toDoubleOrNull()?.let { FlagValue.Double(it) }
                        }
                        is FlagValue.StringV -> {
                            FlagValue.StringV(textValue)
                        }
                        else -> null
                    }
                    
                    if (newValue != null) {
                        onConfirm(newValue)
                    }
                },
                enabled = !isError && textValue.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getValueTypeLabel(value: FlagValue?): String {
    return when (value) {
        is FlagValue.Bool -> "Boolean"
        is FlagValue.Int -> "Integer"
        is FlagValue.Double -> "Double"
        is FlagValue.StringV -> "String"
        is FlagValue.Json -> "JSON"
        null -> "Unknown"
    }
}

