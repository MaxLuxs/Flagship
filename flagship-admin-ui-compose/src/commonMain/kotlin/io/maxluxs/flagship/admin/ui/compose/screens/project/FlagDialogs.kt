package io.maxluxs.flagship.admin.ui.compose.screens.project

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.admin.ui.compose.api.AdminApiClient
import io.maxluxs.flagship.shared.api.RestFlagValue
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import io.maxluxs.flagship.ui.components.components.BrandedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFlagDialog(
    apiClient: AdminApiClient,
    authToken: String,
    projectId: String,
    existingKeys: Set<String>,
    onDismiss: () -> Unit,
    onFlagCreated: (String, RestFlagValue) -> Unit,
    onError: (String) -> Unit
) {
    var flagKey by remember { mutableStateOf("") }
    var flagType by remember { mutableStateOf("bool") }
    var boolValue by remember { mutableStateOf(true) }
    var stringValue by remember { mutableStateOf("") }
    var numberValue by remember { mutableStateOf("0") }
    var jsonValue by remember { mutableStateOf("{}") }
    var isLoading by remember { mutableStateOf(false) }
    var keyError by remember { mutableStateOf<String?>(null) }
    var valueError by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    fun validateKey(): Boolean {
        if (flagKey.isBlank()) {
            keyError = "Flag key cannot be empty"
            return false
        }
        if (!flagKey.matches(Regex("^[a-z0-9_]+$"))) {
            keyError = "Flag key must contain only lowercase letters, numbers, and underscores"
            return false
        }
        if (existingKeys.contains(flagKey)) {
            keyError = "Flag key already exists"
            return false
        }
        keyError = null
        return true
    }
    
    fun validateValue(): Boolean {
        when (flagType) {
            "bool" -> {
                valueError = null
                return true
            }
            "string" -> {
                if (stringValue.isBlank()) {
                    valueError = "String value cannot be empty"
                    return false
                }
                valueError = null
                return true
            }
            "int", "double", "number" -> {
                try {
                    numberValue.toDouble()
                    valueError = null
                    return true
                } catch (e: Exception) {
                    valueError = "Invalid number format"
                    return false
                }
            }
            "json" -> {
                try {
                    kotlinx.serialization.json.Json.parseToJsonElement(jsonValue)
                    valueError = null
                    return true
                } catch (e: Exception) {
                    valueError = "Invalid JSON format: ${e.message}"
                    return false
                }
            }
            else -> {
                valueError = "Unknown flag type"
                return false
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Flag") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = flagKey,
                    onValueChange = { 
                        flagKey = it
                        validateKey()
                    },
                    label = { Text("Flag Key") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = keyError != null,
                    supportingText = keyError?.let { { Text(it) } }
                )
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = flagType,
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
                        listOf("bool", "string", "int", "double", "number", "json").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    flagType = type
                                    expanded = false
                                    validateValue()
                                }
                            )
                        }
                    }
                }
                
                when (flagType) {
                    "bool" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Value: ", modifier = Modifier.weight(1f))
                            Switch(
                                checked = boolValue,
                                onCheckedChange = { boolValue = it }
                            )
                            Text(if (boolValue) "true" else "false")
                        }
                    }
                    "string" -> {
                        OutlinedTextField(
                            value = stringValue,
                            onValueChange = { 
                                stringValue = it
                                validateValue()
                            },
                            label = { Text("Value") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = valueError != null,
                            supportingText = valueError?.let { { Text(it) } }
                        )
                    }
                    "int", "double", "number" -> {
                        OutlinedTextField(
                            value = numberValue,
                            onValueChange = { 
                                numberValue = it
                                validateValue()
                            },
                            label = { Text("Value") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = valueError != null,
                            supportingText = valueError?.let { { Text(it) } }
                        )
                    }
                    "json" -> {
                        OutlinedTextField(
                            value = jsonValue,
                            onValueChange = { 
                                jsonValue = it
                                validateValue()
                            },
                            label = { Text("JSON Value") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            isError = valueError != null,
                            supportingText = valueError?.let { { Text(it) } }
                        )
                    }
                }
            }
        },
        confirmButton = {
            BrandedButton(
                onClick = {
                    if (validateKey() && validateValue()) {
                        isLoading = true
                        scope.launch {
                            try {
                                val jsonValue = when (flagType) {
                                    "bool" -> JsonPrimitive(boolValue)
                                    "string" -> JsonPrimitive(stringValue)
                                    "int" -> JsonPrimitive(numberValue.toIntOrNull() ?: 0)
                                    "double", "number" -> JsonPrimitive(numberValue.toDoubleOrNull() ?: 0.0)
                                    "json" -> kotlinx.serialization.json.Json.parseToJsonElement(jsonValue)
                                    else -> JsonPrimitive("")
                                }
                                
                                val flagValue = RestFlagValue(flagType, jsonValue)
                                apiClient.createFlag(authToken, projectId, flagKey, flagValue)
                                onFlagCreated(flagKey, flagValue)
                            } catch (e: Exception) {
                                onError("Failed to create flag: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading && keyError == null && valueError == null
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFlagDialog(
    apiClient: AdminApiClient,
    authToken: String,
    projectId: String,
    flagKey: String,
    currentFlag: RestFlagValue,
    onDismiss: () -> Unit,
    onFlagUpdated: (RestFlagValue) -> Unit,
    onError: (String) -> Unit
) {
    var flagType by remember { mutableStateOf(currentFlag.type) }
    var boolValue by remember { mutableStateOf(
        if (currentFlag.type == "bool") currentFlag.value.toString().toBoolean() else true
    ) }
    var stringValue by remember { mutableStateOf(
        if (currentFlag.type == "string") currentFlag.value.toString().trim('"') else ""
    ) }
    var numberValue by remember { mutableStateOf(
        if (currentFlag.type in listOf("int", "double", "number")) currentFlag.value.toString() else "0"
    ) }
    var jsonValue by remember { mutableStateOf(
        if (currentFlag.type == "json") currentFlag.value.toString() else "{}"
    ) }
    var isLoading by remember { mutableStateOf(false) }
    var valueError by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    fun validateValue(): Boolean {
        when (flagType) {
            "bool" -> {
                valueError = null
                return true
            }
            "string" -> {
                if (stringValue.isBlank()) {
                    valueError = "String value cannot be empty"
                    return false
                }
                valueError = null
                return true
            }
            "int", "double", "number" -> {
                try {
                    numberValue.toDouble()
                    valueError = null
                    return true
                } catch (e: Exception) {
                    valueError = "Invalid number format"
                    return false
                }
            }
            "json" -> {
                try {
                    kotlinx.serialization.json.Json.parseToJsonElement(jsonValue)
                    valueError = null
                    return true
                } catch (e: Exception) {
                    valueError = "Invalid JSON format: ${e.message}"
                    return false
                }
            }
            else -> {
                valueError = "Unknown flag type"
                return false
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Flag: $flagKey") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = flagType,
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
                        listOf("bool", "string", "int", "double", "number", "json").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    flagType = type
                                    expanded = false
                                    validateValue()
                                }
                            )
                        }
                    }
                }
                
                when (flagType) {
                    "bool" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Value: ", modifier = Modifier.weight(1f))
                            Switch(
                                checked = boolValue,
                                onCheckedChange = { boolValue = it }
                            )
                            Text(if (boolValue) "true" else "false")
                        }
                    }
                    "string" -> {
                        OutlinedTextField(
                            value = stringValue,
                            onValueChange = { 
                                stringValue = it
                                validateValue()
                            },
                            label = { Text("Value") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = valueError != null,
                            supportingText = valueError?.let { { Text(it) } }
                        )
                    }
                    "int", "double", "number" -> {
                        OutlinedTextField(
                            value = numberValue,
                            onValueChange = { 
                                numberValue = it
                                validateValue()
                            },
                            label = { Text("Value") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = valueError != null,
                            supportingText = valueError?.let { { Text(it) } }
                        )
                    }
                    "json" -> {
                        OutlinedTextField(
                            value = jsonValue,
                            onValueChange = { 
                                jsonValue = it
                                validateValue()
                            },
                            label = { Text("JSON Value") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            isError = valueError != null,
                            supportingText = valueError?.let { { Text(it) } }
                        )
                    }
                }
            }
        },
        confirmButton = {
            BrandedButton(
                onClick = {
                    if (validateValue()) {
                        isLoading = true
                        scope.launch {
                            try {
                                val jsonValue = when (flagType) {
                                    "bool" -> JsonPrimitive(boolValue)
                                    "string" -> JsonPrimitive(stringValue)
                                    "int" -> JsonPrimitive(numberValue.toIntOrNull() ?: 0)
                                    "double", "number" -> JsonPrimitive(numberValue.toDoubleOrNull() ?: 0.0)
                                    "json" -> kotlinx.serialization.json.Json.parseToJsonElement(jsonValue)
                                    else -> JsonPrimitive("")
                                }
                                
                                val flagValue = RestFlagValue(flagType, jsonValue)
                                apiClient.updateFlag(authToken, projectId, flagKey, flagValue)
                                onFlagUpdated(flagValue)
                            } catch (e: Exception) {
                                onError("Failed to update flag: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading && valueError == null
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

