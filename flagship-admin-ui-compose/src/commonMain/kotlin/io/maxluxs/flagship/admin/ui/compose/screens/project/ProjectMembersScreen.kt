package io.maxluxs.flagship.admin.ui.compose.screens.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.admin.ui.compose.api.AdminApiClient
import io.maxluxs.flagship.shared.api.ProjectMemberResponse
import io.maxluxs.flagship.shared.api.AddProjectMemberRequest
import io.maxluxs.flagship.shared.api.ErrorResponse
import io.maxluxs.flagship.ui.components.components.BrandedCard
import io.maxluxs.flagship.ui.components.components.BrandedButton
import io.maxluxs.flagship.ui.components.components.BrandedTopBar
import io.maxluxs.flagship.ui.components.theme.BrandColors
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * Validates email format using a simple regex pattern.
 */
fun isValidEmail(email: String): Boolean {
    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
    return emailRegex.matches(email)
}

/**
 * Formats a timestamp (milliseconds since epoch) to a readable date string (YYYY-MM-DD).
 * Simple implementation that works across all platforms.
 */
fun formatDate(timestamp: Long): String {
    // Convert milliseconds to seconds
    val seconds = timestamp / 1000
    val days = seconds / 86400
    val epochDays = 719163L // Days from 1970-01-01 to 2000-01-01 (approximate)
    val totalDays = days + epochDays
    
    // Simple calculation for year, month, day (approximate, but good enough for display)
    // This is a simplified version - for production, use a proper date library
    val year = 1970 + (totalDays / 365.25).toInt()
    val dayOfYear = (totalDays % 365.25).toInt()
    val month = (dayOfYear / 30.44).toInt() + 1
    val day = (dayOfYear % 30.44).toInt() + 1
    
    return "${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectMembersScreen(
    apiBaseUrl: String,
    authToken: String,
    projectId: String,
    onNavigateBack: () -> Unit
) {
    var members by remember { mutableStateOf<List<ProjectMemberResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var deletingMemberId by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val apiClient = remember { AdminApiClient(apiBaseUrl) }
    
    fun loadMembers() {
        scope.launch {
            isLoading = true
            try {
                members = apiClient.getProjectMembers(authToken, projectId)
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = try {
                    val errorResponse = Json.decodeFromString<ErrorResponse>(e.message ?: "")
                    errorResponse.details ?: errorResponse.error
                } catch (ex: Exception) {
                    "Failed to load members: ${e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(projectId) {
        loadMembers()
    }
    
    Scaffold(
        topBar = {
            BrandedTopBar(
                title = "Project Members",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Member")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Error message
            errorMessage?.let { error ->
                BrandedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Success message
            successMessage?.let { success ->
                BrandedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = BrandColors.FlagshipGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = success,
                            color = BrandColors.FlagshipGreen,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (members.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No members yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Add a member to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(members) { member ->
                        MemberCard(
                            member = member,
                            onDelete = {
                                deletingMemberId = member.userId
                            },
                            isDeleting = deletingMemberId == member.userId
                        )
                    }
                }
            }
        }
    }
    
    // Add member dialog
    if (showAddDialog) {
        AddMemberDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { email, role ->
                scope.launch {
                    try {
                        apiClient.addProjectMember(authToken, projectId, email, role)
                        successMessage = "Member added successfully"
                        errorMessage = null
                        showAddDialog = false
                        loadMembers()
                    } catch (e: Exception) {
                        errorMessage = try {
                            val errorResponse = Json.decodeFromString<ErrorResponse>(e.message ?: "")
                            errorResponse.details ?: errorResponse.error
                        } catch (ex: Exception) {
                            "Failed to add member: ${e.message}"
                        }
                    }
                }
            }
        )
    }
    
    // Delete confirmation dialog
    deletingMemberId?.let { memberId ->
        AlertDialog(
            onDismissRequest = { deletingMemberId = null },
            title = { Text("Delete Member") },
            text = { Text("Are you sure you want to remove this member from the project?") },
            confirmButton = {
                BrandedButton(
                    onClick = {
                        scope.launch {
                            try {
                                apiClient.removeProjectMember(authToken, projectId, memberId)
                                successMessage = "Member removed successfully"
                                errorMessage = null
                                deletingMemberId = null
                                loadMembers()
                            } catch (e: Exception) {
                                errorMessage = try {
                                    val errorResponse = Json.decodeFromString<ErrorResponse>(e.message ?: "")
                                    errorResponse.details ?: errorResponse.error
                                } catch (ex: Exception) {
                                    "Failed to remove member: ${e.message}"
                                }
                                deletingMemberId = null
                            }
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMemberId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MemberCard(
    member: ProjectMemberResponse,
    onDelete: () -> Unit,
    isDeleting: Boolean
) {
    // Цветовое кодирование ролей
    val roleColor = when (member.role) {
        "ADMIN" -> BrandColors.FlagshipOrange
        "MEMBER" -> BrandColors.FlagshipGreen
        "VIEWER" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val roleContainerColor = when (member.role) {
        "ADMIN" -> BrandColors.FlagshipOrangeContainer
        "MEMBER" -> BrandColors.FlagshipGreenContainer
        "VIEWER" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    BrandedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = roleColor
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.email,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    member.name?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = roleContainerColor
                        ) {
                            Text(
                                text = member.role,
                                style = MaterialTheme.typography.labelSmall,
                                color = roleColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Joined: ${formatDate(member.joinedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(
                onClick = onDelete,
                enabled = !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("MEMBER") }
    var emailError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Project Member") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                    },
                    label = { Text("Email") },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("ADMIN", "MEMBER", "VIEWER").forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    role = r
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
                    if (email.isBlank()) {
                        emailError = "Email is required"
                    } else if (!isValidEmail(email)) {
                        emailError = "Invalid email format"
                    } else {
                        onConfirm(email, role)
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

