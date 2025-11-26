package io.maxluxs.flagship.server.analytics

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.maxluxs.flagship.shared.api.ProviderMetricsRequest
import io.maxluxs.flagship.server.auth.ProjectAccessService
import io.maxluxs.flagship.server.auth.getUserId
import io.ktor.server.auth.authenticate
import java.util.*

fun Routing.providerAnalyticsRoutes(
    providerAnalyticsService: ProviderAnalyticsService,
    accessService: ProjectAccessService
) {
    route("/api/projects/{projectId}/analytics/providers") {
        // Public endpoint for SDK to send provider metrics
        post {
            val projectIdStr = call.parameters["projectId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Project ID required"))
                return@post
            }
            
            val projectId = try {
                UUID.fromString(projectIdStr)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid project ID"))
                return@post
            }
            
            val request = try {
                call.receive<ProviderMetricsRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body: ${e.message}"))
                return@post
            }
            
            try {
                providerAnalyticsService.recordMetrics(projectId, request)
                call.respond(HttpStatusCode.Created, mapOf("status" to "recorded"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to record metrics: ${e.message}"))
            }
        }
        
        // Admin endpoints (require auth)
        authenticate("jwt-auth") {
            get {
                val projectIdStr = call.parameters["projectId"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Project ID required"))
                    return@get
                }
                
                val projectId = try {
                    UUID.fromString(projectIdStr)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid project ID"))
                    return@get
                }
                
                val userId = UUID.fromString(call.getUserId() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@get
                })
                
                // Check access
                if (!accessService.checkProjectAccess(userId, projectId)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                    return@get
                }
                
                val providerName = call.request.queryParameters["provider"]
                
                try {
                    val metrics = providerAnalyticsService.getLatestMetrics(projectId, providerName)
                    call.respond(metrics)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to get metrics: ${e.message}"))
                }
            }
            
            get("/health") {
                val projectIdStr = call.parameters["projectId"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Project ID required"))
                    return@get
                }
                
                val projectId = try {
                    UUID.fromString(projectIdStr)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid project ID"))
                    return@get
                }
                
                val userId = UUID.fromString(call.getUserId() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@get
                })
                
                // Check access
                if (!accessService.checkProjectAccess(userId, projectId)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                    return@get
                }
                
                try {
                    val healthStatus = providerAnalyticsService.getProviderHealthStatus(projectId)
                    call.respond(healthStatus)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to get health status: ${e.message}"))
                }
            }
            
            get("/{providerName}") {
                val projectIdStr = call.parameters["projectId"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Project ID required"))
                    return@get
                }
                
                val projectId = try {
                    UUID.fromString(projectIdStr)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid project ID"))
                    return@get
                }
                
                val providerName = call.parameters["providerName"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Provider name required"))
                    return@get
                }
                
                val userId = UUID.fromString(call.getUserId() ?: run {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                    return@get
                })
                
                // Check access
                if (!accessService.checkProjectAccess(userId, projectId)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                    return@get
                }
                
                val startTime = call.request.queryParameters["startTime"]?.toLongOrNull()
                val endTime = call.request.queryParameters["endTime"]?.toLongOrNull()
                
                try {
                    val history = providerAnalyticsService.getProviderMetricsHistory(projectId, providerName, startTime, endTime)
                    call.respond(history)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to get metrics history: ${e.message}"))
                }
            }
        }
    }
}

