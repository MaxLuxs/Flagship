@file:OptIn(ExperimentalMaterial3Api::class)

package io.maxluxs.flagship.admin.ui.compose.screens.project

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.admin.ui.compose.api.AdminApiClient
import io.maxluxs.flagship.shared.api.ProjectResponse
import io.maxluxs.flagship.shared.api.ProjectMemberResponse
import kotlinx.coroutines.launch
import io.maxluxs.flagship.admin.ui.compose.screens.project.ProvidersScreen
import io.maxluxs.flagship.ui.components.components.BrandedTopBar
import io.maxluxs.flagship.ui.components.components.BrandedCard
import io.maxluxs.flagship.ui.components.components.BrandedButton
import io.maxluxs.flagship.ui.components.theme.BrandColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    apiBaseUrl: String,
    authToken: String,
    projectId: String,
    onNavigateToFlags: () -> Unit,
    onNavigateToExperiments: () -> Unit,
    onNavigateToApiKeys: () -> Unit,
    onNavigateToMembers: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToAuditLog: () -> Unit = {},
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var project by remember { mutableStateOf<ProjectResponse?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    val scope = rememberCoroutineScope()
    val apiClient = remember { AdminApiClient(apiBaseUrl) }
    
    LaunchedEffect(projectId) {
        scope.launch {
            try {
                project = apiClient.getProject(authToken, projectId)
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            BrandedTopBar(
                title = project?.name ?: "Project Details",
                showLogo = true,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Project statistics cards
            val currentProject = project
            if (currentProject != null && !isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrandedCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Project",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentProject.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                    BrandedCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Slug",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentProject.slug,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Flags") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Experiments") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("API Keys") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Providers") }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Members") }
                )
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    text = { Text("Analytics") }
                )
                Tab(
                    selected = selectedTab == 6,
                    onClick = { selectedTab = 6 },
                    text = { Text("Audit Log") }
                )
                Tab(
                    selected = selectedTab == 7,
                    onClick = { selectedTab = 7 },
                    text = { Text("Settings") }
                )
            }
            
            when (selectedTab) {
                0 -> {
                    FlagsScreen(
                        apiBaseUrl = apiBaseUrl,
                        authToken = authToken,
                        projectId = projectId,
                        onNavigateBack = onNavigateBack
                    )
                }
                1 -> {
                    ExperimentsScreen(
                        apiBaseUrl = apiBaseUrl,
                        authToken = authToken,
                        projectId = projectId,
                        onNavigateBack = onNavigateBack
                    )
                }
                2 -> {
                    ApiKeysScreen(
                        apiBaseUrl = apiBaseUrl,
                        authToken = authToken,
                        projectId = projectId,
                        onNavigateBack = onNavigateBack
                    )
                }
                3 -> {
                    ProvidersScreen(
                        apiBaseUrl = apiBaseUrl,
                        authToken = authToken,
                        projectId = projectId,
                        onNavigateBack = onNavigateBack
                    )
                }
                4 -> {
                    if (onNavigateToMembers != {}) {
                        onNavigateToMembers()
                    } else {
                        // Fallback: show members inline
                        Text("Members tab - navigation not configured")
                    }
                }
                5 -> {
                    if (onNavigateToAnalytics != {}) {
                        onNavigateToAnalytics()
                    } else {
                        // Fallback: show analytics inline
                        Text("Analytics tab - navigation not configured")
                    }
                }
                6 -> {
                    if (onNavigateToAuditLog != {}) {
                        onNavigateToAuditLog()
                    } else {
                        // Fallback: show audit log inline
                        Text("Audit Log tab - navigation not configured")
                    }
                }
                7 -> {
                    ProjectSettingsTab(
                        apiClient = apiClient,
                        authToken = authToken,
                        project = project,
                        onProjectUpdated = { updatedProject ->
                            project = updatedProject
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectSettingsTab(
    apiClient: AdminApiClient,
    authToken: String,
    project: ProjectResponse?,
    onProjectUpdated: (ProjectResponse) -> Unit
) {
    var name by remember { mutableStateOf(project?.name ?: "") }
    var description by remember { mutableStateOf(project?.description ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(project) {
        name = project?.name ?: ""
        description = project?.description ?: ""
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (project != null) {
            Text(
                text = "Project Settings",
                style = MaterialTheme.typography.headlineSmall
            )
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Project Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = description ?: "",
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            
            BrandedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Project Information",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "ID: ${project.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Slug: ${project.slug}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Owner ID: ${project.ownerId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            BrandedButton(
                onClick = {
                    if (name.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val updated = apiClient.updateProject(
                                    authToken, 
                                    project.id, 
                                    if (name != project.name) name else null,
                                    if (description != project.description) description else null
                                )
                                onProjectUpdated(updated)
                                successMessage = "Project settings updated successfully"
                            } catch (e: Exception) {
                                errorMessage = "Failed to update project: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && name.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Save Changes")
                }
            }
            
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            if (successMessage != null) {
                Text(
                    text = successMessage!!,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Members management section
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            Text(
                text = "Project Members",
                style = MaterialTheme.typography.titleLarge
            )
            
            ProjectMembersSection(
                apiClient = apiClient,
                authToken = authToken,
                projectId = project.id
            )
            
            // Delete project section
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            Text(
                text = "Danger Zone",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            
            var showDeleteConfirm by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete Project")
            }
            
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("Delete Project?") },
                    text = {
                        Text("This action cannot be undone. All flags, experiments, and data will be permanently deleted.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        apiClient.deleteProject(authToken, project.id)
                                        // Navigate back - handled by parent
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to delete project: ${e.message}"
                                        showDeleteConfirm = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun ProjectMembersSection(
    apiClient: AdminApiClient,
    authToken: String,
    projectId: String
) {
    var members by remember { mutableStateOf<List<ProjectMemberResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    fun loadMembers() {
        scope.launch {
            isLoading = true
            try {
                members = apiClient.getProjectMembers(authToken, projectId)
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Failed to load members: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(projectId) {
        loadMembers()
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Members (${members.size})")
            BrandedButton(onClick = { showAddDialog = true }) {
                Text("Add Member")
            }
        }
        
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            members.forEach { member ->
                BrandedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.email,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Role: ${member.role}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (member.role != "OWNER") {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            apiClient.removeProjectMember(
                                                authToken,
                                                projectId,
                                                member.userId
                                            )
                                            loadMembers()
                                        } catch (e: Exception) {
                                            errorMessage = "Failed to remove member: ${e.message}"
                                        }
                                    }
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
                }
            }
        }
        
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
    
    if (showAddDialog) {
        AddMemberDialog(
            apiClient = apiClient,
            authToken = authToken,
            projectId = projectId,
            onDismiss = { showAddDialog = false },
            onMemberAdded = {
                loadMembers()
                showAddDialog = false
            },
            onError = { errorMessage = it }
        )
    }
}

@Composable
fun AddMemberDialog(
    apiClient: AdminApiClient,
    authToken: String,
    projectId: String,
    onDismiss: () -> Unit,
    onMemberAdded: () -> Unit,
    onError: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("MEMBER") }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
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
                    if (email.isNotBlank()) {
                        isLoading = true
                        scope.launch {
                            try {
                                apiClient.addProjectMember(authToken, projectId, email, role)
                                onMemberAdded()
                            } catch (e: Exception) {
                                onError("Failed to add member: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading && email.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Add")
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

