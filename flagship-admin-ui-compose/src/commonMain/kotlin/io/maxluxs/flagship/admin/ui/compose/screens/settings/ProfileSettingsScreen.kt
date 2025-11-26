package io.maxluxs.flagship.admin.ui.compose.screens.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.admin.ui.compose.api.AdminApiClient
import io.maxluxs.flagship.shared.api.UserResponse
import io.maxluxs.flagship.ui.components.components.BrandedButton
import io.maxluxs.flagship.ui.components.components.BrandedCard
import io.maxluxs.flagship.ui.components.components.BrandedTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    apiBaseUrl: String,
    authToken: String,
    currentUser: UserResponse?,
    onNavigateBack: () -> Unit,
    onUserUpdated: (UserResponse) -> Unit
) {
    var name by remember { mutableStateOf(currentUser?.name ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var user by remember { mutableStateOf(currentUser) }
    
    val scope = rememberCoroutineScope()
    val apiClient = remember { AdminApiClient(apiBaseUrl) }
    
    LaunchedEffect(authToken) {
        if (user == null) {
            scope.launch {
                try {
                    user = apiClient.getUser(authToken)
                    name = user?.name ?: ""
                } catch (e: Exception) {
                    errorMessage = "Failed to load user profile: ${e.message}"
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            BrandedTopBar(
                title = "Profile Settings",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = user != null,
                enter = fadeIn() + slideInVertically(
                    initialOffsetY = { -20 },
                    animationSpec = tween(300)
                ),
                exit = fadeOut() + slideOutVertically(
                    targetOffsetY = { -20 },
                    animationSpec = tween(300)
                )
            ) {
                if (user != null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Profile Information",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        OutlinedTextField(
                            value = user!!.email,
                            onValueChange = {},
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false
                        )
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Enter your name") }
                        )
                        
                        BrandedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Account Information",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "User ID: ${user!!.id}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Role: ${if (user!!.isAdmin) "Administrator" else "User"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        BrandedButton(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                successMessage = null
                                scope.launch {
                                    try {
                                        val updated = apiClient.updateUser(
                                            authToken,
                                            if (name.isNotBlank() && name != user!!.name) name else null
                                        )
                                        user = updated
                                        name = updated.name ?: ""
                                        onUserUpdated(updated)
                                        successMessage = "Profile updated successfully"
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to update profile: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && name.isNotBlank() && name != user!!.name
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Save Changes")
                            }
                        }
                        
                        // Error message with animation
                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = fadeIn() + slideInVertically(
                                initialOffsetY = { -10 },
                                animationSpec = tween(300)
                            ),
                            exit = fadeOut() + slideOutVertically(
                                targetOffsetY = { -10 },
                                animationSpec = tween(300)
                            )
                        ) {
                            errorMessage?.let { message ->
                                BrandedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Success message with animation
                        AnimatedVisibility(
                            visible = successMessage != null,
                            enter = fadeIn() + slideInVertically(
                                initialOffsetY = { -10 },
                                animationSpec = tween(300)
                            ),
                            exit = fadeOut() + slideOutVertically(
                                targetOffsetY = { -10 },
                                animationSpec = tween(300)
                            )
                        ) {
                            successMessage?.let { message ->
                                LaunchedEffect(message) {
                                    delay(3000)
                                    successMessage = null
                                }
                                BrandedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Loading state
            AnimatedVisibility(
                visible = user == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

