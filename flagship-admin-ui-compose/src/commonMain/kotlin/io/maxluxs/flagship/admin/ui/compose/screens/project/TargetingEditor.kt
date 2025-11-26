@file:OptIn(ExperimentalMaterial3Api::class)

package io.maxluxs.flagship.admin.ui.compose.screens.project

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.shared.api.RestTargeting

@Composable
fun SimpleTargetingEditor(
    targeting: RestTargeting?,
    onTargetingChange: (RestTargeting?) -> Unit
) {
    var targetingType by remember { mutableStateOf(targeting?.type ?: "region_in") }
    var regions by remember { mutableStateOf(targeting?.regions?.joinToString(", ") ?: "") }
    var attributeKey by remember { mutableStateOf(targeting?.key ?: "") }
    var attributeValue by remember { mutableStateOf(targeting?.value ?: "") }
    var version by remember { mutableStateOf(targeting?.version ?: "") }
    
    LaunchedEffect(targetingType, regions, attributeKey, attributeValue, version) {
        val newTargeting = when (targetingType) {
            "region_in" -> {
                val regionList = regions.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (regionList.isNotEmpty()) {
                    RestTargeting(type = "region_in", regions = regionList)
                } else null
            }
            "attribute_equals" -> {
                if (attributeKey.isNotBlank() && attributeValue.isNotBlank()) {
                    RestTargeting(type = "attribute_equals", key = attributeKey, value = attributeValue)
                } else null
            }
            "app_version_gte" -> {
                if (version.isNotBlank()) {
                    RestTargeting(type = "app_version_gte", version = version)
                } else null
            }
            else -> null
        }
        onTargetingChange(newTargeting)
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = targetingType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Targeting Type") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                listOf("region_in", "attribute_equals", "app_version_gte").forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            targetingType = type
                            expanded = false
                        }
                    )
                }
            }
        }
        
        when (targetingType) {
            "region_in" -> {
                OutlinedTextField(
                    value = regions,
                    onValueChange = { regions = it },
                    label = { Text("Regions (comma-separated, e.g., US, CA, GB)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "attribute_equals" -> {
                OutlinedTextField(
                    value = attributeKey,
                    onValueChange = { attributeKey = it },
                    label = { Text("Attribute Key") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = attributeValue,
                    onValueChange = { attributeValue = it },
                    label = { Text("Attribute Value") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "app_version_gte" -> {
                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text("Minimum App Version") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

