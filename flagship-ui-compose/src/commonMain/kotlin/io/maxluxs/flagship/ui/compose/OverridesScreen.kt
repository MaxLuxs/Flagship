package io.maxluxs.flagship.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.model.FlagValue

@Composable
fun OverridesScreen(manager: FlagsManager) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    val overrides by remember { derivedStateOf { manager.listOverrides() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with actions
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Overrides",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${overrides.size} override(s) active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (overrides.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showClearAllDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text("Clear All")
                        }
                    }
                    Button(onClick = { showAddDialog = true }) {
                        Text("+ Add")
                    }
                }
            }
        }

        if (overrides.isEmpty()) {
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
                        text = "⚙️",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = "No overrides set",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add custom overrides for testing",
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
                items(
                    items = overrides.entries.toList(),
                    key = { it.key }
                ) { (key, value) ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        OverrideItem(
                            key = key,
                            value = value,
                            onClear = { manager.clearOverride(key) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddOverrideDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { key, value ->
                manager.setOverride(key, value)
                showAddDialog = false
            }
        )
    }
    
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All Overrides?") },
            text = { Text("This will remove all ${overrides.size} override(s). This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        overrides.keys.forEach { manager.clearOverride(it) }
                        showClearAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun OverrideItem(
    key: String,
    value: FlagValue,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = getValueTypeLabel(value),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = key,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = formatValue(value),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            IconButton(onClick = onClear) {
                Text(
                    "✕",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun getValueTypeLabel(value: FlagValue): String {
    return when (value) {
        is FlagValue.Bool -> "BOOL"
        is FlagValue.Int -> "INT"
        is FlagValue.Double -> "DOUBLE"
        is FlagValue.StringV -> "STRING"
        is FlagValue.Json -> "JSON"
    }
}

@Composable
fun AddOverrideDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, FlagValue) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var valueType by remember { mutableStateOf("bool") }
    var boolValue by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Override") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Flag Key") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = valueType == "bool",
                        onClick = { valueType = "bool" },
                        label = { Text("Bool") }
                    )
                    FilterChip(
                        selected = valueType == "int",
                        onClick = { valueType = "int" },
                        label = { Text("Int") }
                    )
                    FilterChip(
                        selected = valueType == "double",
                        onClick = { valueType = "double" },
                        label = { Text("Double") }
                    )
                    FilterChip(
                        selected = valueType == "string",
                        onClick = { valueType = "string" },
                        label = { Text("String") }
                    )
                }

                when (valueType) {
                    "bool" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Value: ")
                            Switch(
                                checked = boolValue,
                                onCheckedChange = { boolValue = it }
                            )
                            Text(if (boolValue) "true" else "false")
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            label = { Text("Value") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (key.isNotBlank()) {
                        val value = when (valueType) {
                            "bool" -> FlagValue.Bool(boolValue)
                            "int" -> FlagValue.Int(textValue.toIntOrNull() ?: 0)
                            "double" -> FlagValue.Double(textValue.toDoubleOrNull() ?: 0.0)
                            "string" -> FlagValue.StringV(textValue)
                            else -> FlagValue.StringV(textValue)
                        }
                        onConfirm(key, value)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatValue(value: FlagValue): String {
    return when (value) {
        is FlagValue.Bool -> "Boolean: ${value.value}"
        is FlagValue.Int -> "Integer: ${value.value}"
        is FlagValue.Double -> "Double: ${value.value}"
        is FlagValue.StringV -> "String: ${value.value}"
        is FlagValue.Json -> "JSON: ${value.value}"
    }
}

