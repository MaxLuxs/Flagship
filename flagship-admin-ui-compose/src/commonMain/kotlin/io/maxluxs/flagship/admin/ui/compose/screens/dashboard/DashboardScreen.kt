package io.maxluxs.flagship.admin.ui.compose.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.admin.ui.compose.api.AdminApiClient
import io.maxluxs.flagship.shared.api.ProjectResponse
import io.maxluxs.flagship.ui.components.components.BrandedButton
import io.maxluxs.flagship.ui.components.components.BrandedCard
import io.maxluxs.flagship.ui.components.components.BrandedLogo
import io.maxluxs.flagship.ui.components.components.BrandedTopBar
import io.maxluxs.flagship.ui.components.theme.BrandColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    apiBaseUrl: String,
    authToken: String,
    userEmail: String?,
    userName: String?,
    onProjectSelected: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    var projects by remember { mutableStateOf<List<ProjectResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val apiClient = remember { AdminApiClient(apiBaseUrl) }
    
    LaunchedEffect(authToken) {
        scope.launch {
            try {
                projects = apiClient.getProjects(authToken)
            } catch (e: Exception) {
                errorMessage = "Failed to load projects: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            BrandedTopBar(
                title = "Projects",
                showLogo = true,
                actions = {
                    if (userEmail != null) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Project")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (projects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BrandedLogo(size = 64.dp)
                        Text(
                            text = "No projects yet",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "Create your first project to get started",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BrandedButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Create Project")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(projects) { project ->
                        ProjectCard(
                            project = project,
                            apiClient = apiClient,
                            authToken = authToken,
                            onProjectSelected = { onProjectSelected(project.id) }
                        )
                    }
                }
            }
        }
    }
    
    if (showCreateDialog) {
        CreateProjectDialog(
            apiClient = apiClient,
            authToken = authToken,
            onDismiss = { showCreateDialog = false },
            onProjectCreated = { project ->
                projects = projects + project
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun CreateProjectDialog(
    apiClient: AdminApiClient,
    authToken: String,
    onDismiss: () -> Unit,
    onProjectCreated: (ProjectResponse) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Project") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = slug,
                    onValueChange = { slug = it },
                    label = { Text("Slug") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            BrandedButton(
                onClick = {
                    if (name.isNotBlank() && slug.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val project = apiClient.createProject(
                                    authToken,
                                    name,
                                    slug,
                                    description.takeIf { it.isNotBlank() }
                                )
                                onProjectCreated(project)
                            } catch (e: Exception) {
                                errorMessage = "Failed to create project: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading && name.isNotBlank() && slug.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
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
fun ProjectCard(
    project: ProjectResponse,
    apiClient: AdminApiClient,
    authToken: String,
    onProjectSelected: () -> Unit
) {
    var flagsCount by remember { mutableStateOf<Int?>(null) }
    var experimentsCount by remember { mutableStateOf<Int?>(null) }
    var isLoadingStats by remember { mutableStateOf(true) }
    
    LaunchedEffect(project.id) {
        isLoadingStats = true
        try {
            val flags = apiClient.getFlags(authToken, project.id)
            val experiments = apiClient.getExperiments(authToken, project.id)
            flagsCount = flags.size
            experimentsCount = experiments.size
        } catch (e: Exception) {
            // Если не удалось загрузить, оставляем null
            flagsCount = null
            experimentsCount = null
        } finally {
            isLoadingStats = false
        }
    }
    
    BrandedCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clickable { onProjectSelected() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = project.slug,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            val description = project.description
            if (description != null && description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                    maxLines = 2
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Визуальные индикаторы количества флагов и экспериментов
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Индикатор флагов
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = BrandColors.FlagshipGreen
                    )
                    if (isLoadingStats) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = flagsCount?.toString() ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = BrandColors.FlagshipGreen
                        )
                    }
                    Text(
                        text = "flags",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Индикатор экспериментов
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Science,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = BrandColors.FlagshipOrange
                    )
                    if (isLoadingStats) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = experimentsCount?.toString() ?: "-",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = BrandColors.FlagshipOrange
                        )
                    }
                    Text(
                        text = "experiments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

