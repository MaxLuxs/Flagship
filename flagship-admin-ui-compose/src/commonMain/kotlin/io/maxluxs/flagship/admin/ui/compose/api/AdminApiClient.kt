package io.maxluxs.flagship.admin.ui.compose.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.maxluxs.flagship.shared.api.*
import io.maxluxs.flagship.shared.api.FlagResponse
import io.maxluxs.flagship.shared.api.ProviderMetricsData
import io.maxluxs.flagship.shared.api.ProviderHealthStatus
import io.maxluxs.flagship.shared.api.ErrorResponse
import kotlinx.serialization.Serializable

/**
 * API client for Flagship Admin Panel.
 * Handles all HTTP requests to the Flagship server.
 */
expect fun createDefaultHttpClient(): HttpClient

class AdminApiClient(
    private val baseUrl: String,
    private val httpClient: HttpClient = createDefaultHttpClient()
) {
    
    // Authentication
    suspend fun login(email: String, password: String): AuthResponse {
        val response = httpClient.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }
        return response.body()
    }
    
    suspend fun register(email: String, password: String, name: String? = null): AuthResponse {
        val response = httpClient.post("$baseUrl/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, password, name))
        }
        return response.body()
    }
    
    // Projects
    suspend fun getProjects(token: String): List<ProjectResponse> {
        val response = httpClient.get("$baseUrl/api/admin/projects") {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    suspend fun createProject(token: String, name: String, slug: String, description: String? = null): ProjectResponse {
        val response = httpClient.post("$baseUrl/api/admin/projects") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateProjectRequest(name, slug, description))
        }
        return response.body()
    }
    
    suspend fun getProject(token: String, projectId: String): ProjectResponse {
        val response = httpClient.get("$baseUrl/api/admin/projects/$projectId") {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    suspend fun updateProject(token: String, projectId: String, name: String? = null, description: String? = null): ProjectResponse {
        val response = httpClient.put("$baseUrl/api/admin/projects/$projectId") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(UpdateProjectRequest(name, description))
        }
        return response.body()
    }
    
    // Flags
    suspend fun getFlags(token: String, projectId: String): Map<String, RestFlagValue> {
        val response = httpClient.get("$baseUrl/api/projects/$projectId/flags") {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    suspend fun getFlagsDetailed(token: String, projectId: String): List<FlagResponse> {
        val response = httpClient.get("$baseUrl/api/projects/$projectId/flags-detailed") {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    suspend fun toggleFlag(token: String, projectId: String, key: String, isEnabled: Boolean): FlagResponse {
        val response = httpClient.request("$baseUrl/api/projects/$projectId/flags/$key/toggle") {
            method = HttpMethod.Patch
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(mapOf("isEnabled" to isEnabled))
        }
        return response.body()
    }
    
    suspend fun createFlag(
        token: String, 
        projectId: String, 
        key: String, 
        flagValue: RestFlagValue,
        description: String? = null,
        isEnabled: Boolean = true
    ) {
        // Try extended request first, fallback to simple if backend doesn't support
        try {
            httpClient.post("$baseUrl/api/projects/$projectId/flags") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(CreateFlagRequest(flagValue, description, isEnabled))
            }
        } catch (e: Exception) {
            // Fallback to simple request if extended not supported
            httpClient.post("$baseUrl/api/projects/$projectId/flags") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf(key to flagValue))
            }
        }
    }
    
    suspend fun updateFlag(
        token: String, 
        projectId: String, 
        key: String, 
        flagValue: RestFlagValue,
        description: String? = null,
        isEnabled: Boolean? = null
    ) {
        // Try extended request first, fallback to simple if backend doesn't support
        try {
            httpClient.put("$baseUrl/api/projects/$projectId/flags/$key") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(UpdateFlagRequest(flagValue, description, isEnabled))
            }
        } catch (e: Exception) {
            // Fallback to simple request if extended not supported
            httpClient.put("$baseUrl/api/projects/$projectId/flags/$key") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(flagValue)
            }
        }
    }
    
    suspend fun deleteFlag(token: String, projectId: String, key: String) {
        httpClient.delete("$baseUrl/api/projects/$projectId/flags/$key") {
            header("Authorization", "Bearer $token")
        }
    }
    
    // Experiments
    suspend fun getExperiments(token: String, projectId: String): Map<String, RestExperiment> {
        val response = httpClient.get("$baseUrl/api/projects/$projectId/experiments") {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    suspend fun createExperiment(
        token: String,
        projectId: String,
        key: String,
        experiment: RestExperiment,
        description: String? = null,
        isActive: Boolean = true
    ) {
        // Try extended request first, fallback to simple if backend doesn't support
        try {
            httpClient.post("$baseUrl/api/projects/$projectId/experiments") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(CreateExperimentRequest(experiment, description, isActive))
            }
        } catch (e: Exception) {
            // Fallback to simple request if extended not supported
            httpClient.post("$baseUrl/api/projects/$projectId/experiments") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf(key to experiment))
            }
        }
    }
    
    suspend fun updateExperiment(
        token: String,
        projectId: String,
        key: String,
        experiment: RestExperiment,
        description: String? = null,
        isActive: Boolean? = null
    ) {
        // Try extended request first, fallback to simple if backend doesn't support
        try {
            httpClient.put("$baseUrl/api/projects/$projectId/experiments/$key") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(UpdateExperimentRequest(experiment, description, isActive))
            }
        } catch (e: Exception) {
            // Fallback to simple request if extended not supported
            httpClient.put("$baseUrl/api/projects/$projectId/experiments/$key") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(experiment)
            }
        }
    }
    
    suspend fun getExperimentsDetailed(token: String, projectId: String): List<ExperimentResponse> {
        val response = httpClient.get("$baseUrl/api/projects/$projectId/experiments-detailed") {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    suspend fun deleteExperiment(token: String, projectId: String, key: String) {
        httpClient.delete("$baseUrl/api/projects/$projectId/experiments/$key") {
            header("Authorization", "Bearer $token")
        }
    }
    
    // API Keys
    suspend fun getApiKeys(token: String, projectId: String): List<ApiKeyResponse> {
        val response = httpClient.get("$baseUrl/api/admin/projects/$projectId/api-keys") {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    suspend fun createApiKey(token: String, projectId: String, name: String, type: String, expirationDays: Int? = null): ApiKeyResponse {
        val response = httpClient.post("$baseUrl/api/admin/projects/$projectId/api-keys") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateApiKeyRequest(name, type, expirationDays))
        }
        return response.body()
    }
    
    suspend fun deleteApiKey(token: String, projectId: String, keyId: String) {
        httpClient.delete("$baseUrl/api/admin/projects/$projectId/api-keys/$keyId") {
            header("Authorization", "Bearer $token")
        }
    }
    
    // Project members
    suspend fun getProjectMembers(token: String, projectId: String): List<ProjectMemberResponse> {
        val response = httpClient.get("$baseUrl/api/admin/projects/$projectId/members") {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    suspend fun addProjectMember(token: String, projectId: String, email: String, role: String): ProjectMemberResponse {
        val response = httpClient.post("$baseUrl/api/admin/projects/$projectId/members") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(AddProjectMemberRequest(email, role))
        }
        return response.body()
    }
    
    suspend fun removeProjectMember(token: String, projectId: String, memberId: String) {
        httpClient.delete("$baseUrl/api/admin/projects/$projectId/members/$memberId") {
            header("Authorization", "Bearer $token")
        }
    }
    
    // Delete project
    suspend fun deleteProject(token: String, projectId: String) {
        httpClient.delete("$baseUrl/api/admin/projects/$projectId?confirm=true") {
            header("Authorization", "Bearer $token")
        }
    }
    
    // User profile
    suspend fun getUser(token: String): UserResponse {
        val response = httpClient.get("$baseUrl/api/auth/user") {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    suspend fun updateUser(token: String, name: String?): UserResponse {
        val response = httpClient.put("$baseUrl/api/auth/user") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(UpdateUserRequest(name))
        }
        return response.body()
    }
    
    // Provider Analytics
    suspend fun getProviderMetrics(token: String, projectId: String, providerName: String? = null): List<ProviderMetricsData> {
        val url = if (providerName != null) {
            "$baseUrl/api/projects/$projectId/analytics/providers?provider=$providerName"
        } else {
            "$baseUrl/api/projects/$projectId/analytics/providers"
        }
        val response = httpClient.get(url) {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    suspend fun getProviderHealthStatus(token: String, projectId: String): List<ProviderHealthStatus> {
        val response = httpClient.get("$baseUrl/api/projects/$projectId/analytics/providers/health") {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    suspend fun getProviderMetricsHistory(
        token: String,
        projectId: String,
        providerName: String,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<ProviderMetricsData> {
        val url = buildString {
            append("$baseUrl/api/projects/$projectId/analytics/providers/$providerName")
            if (startTime != null || endTime != null) {
                append("?")
                if (startTime != null) append("startTime=$startTime")
                if (startTime != null && endTime != null) append("&")
                if (endTime != null) append("endTime=$endTime")
            }
        }
        val response = httpClient.get(url) {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    // Analytics
    suspend fun getAnalyticsOverview(token: String, projectId: String, period: String = "24h"): AnalyticsOverview {
        val response = httpClient.get("$baseUrl/api/projects/$projectId/analytics/overview?period=$period") {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    suspend fun getFlagStats(token: String, projectId: String): List<FlagStats> {
        val response = httpClient.get("$baseUrl/api/projects/$projectId/analytics/flags") {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    suspend fun getExperimentStats(token: String, projectId: String): List<ExperimentStats> {
        val response = httpClient.get("$baseUrl/api/projects/$projectId/analytics/experiments") {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
    
    // Audit logs
    suspend fun getAuditLogs(
        token: String,
        projectId: String,
        limit: Int = 100,
        offset: Int = 0,
        actionType: String? = null
    ): List<AuditLogEntry> {
        val url = buildString {
            append("$baseUrl/api/admin/projects/$projectId/audit")
            append("?limit=$limit&offset=$offset")
            if (actionType != null) {
                append("&action=$actionType")
            }
        }
        val response = httpClient.get(url) {
            header("Authorization", "Bearer $token")
        }
        return response.body()
    }
}

