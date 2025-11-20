package io.maxluxs.flagship.sample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSelectionScreen(
    currentProvider: ProviderType,
    onProviderSelected: (ProviderType) -> Unit,
    onContinue: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Feature Flag Provider") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Choose your feature flag backend:",
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                "This determines where Flagship fetches flag configurations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Provider cards
            ProviderCard(
                type = ProviderType.MOCK,
                icon = Icons.Default.Science,
                isSelected = selectedProvider == ProviderType.MOCK,
                onClick = { selectedProvider = ProviderType.MOCK }
            )
            
            ProviderCard(
                type = ProviderType.REST,
                icon = Icons.Default.Api,
                isSelected = selectedProvider == ProviderType.REST,
                onClick = { selectedProvider = ProviderType.REST }
            )
            
            ProviderCard(
                type = ProviderType.FIREBASE,
                icon = Icons.Default.Cloud,
                isSelected = selectedProvider == ProviderType.FIREBASE,
                onClick = { selectedProvider = ProviderType.FIREBASE }
            )
            
            ProviderCard(
                type = ProviderType.LAUNCHDARKLY,
                icon = Icons.Default.Rocket,
                isSelected = selectedProvider == ProviderType.LAUNCHDARKLY,
                onClick = { selectedProvider = ProviderType.LAUNCHDARKLY }
            )
            
            Spacer(Modifier.weight(1f))
            
            // Setup warning
            if (selectedProvider.requiresSetup) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Column {
                            Text(
                                "Setup Required",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                "You need to configure ${selectedProvider.displayName} in the app code before using it.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedProvider != currentProvider) {
                    OutlinedButton(
                        onClick = { selectedProvider = currentProvider },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            showConfirmDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Switch Provider")
                    }
                } else {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
    
    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Switch Provider?") },
            text = {
                Text("The app will reinitialize with ${selectedProvider.displayName}. Current flag cache will be cleared.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onProviderSelected(selectedProvider)
                    }
                ) {
                    Text("Switch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProviderCard(
    type: ProviderType,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    type.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

