package io.maxluxs.flagship.server.analytics

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import java.util.*
import io.maxluxs.flagship.shared.api.AnalyticsEvent
import io.maxluxs.flagship.server.auth.getUserId
import io.maxluxs.flagship.server.auth.ProjectAccessService

@Serializable
data class AnalyticsEventRequest(
    val eventType: String,
    val entityType: String? = null,
    val entityId: String? = null,
    val attributes: Map<String, String> = emptyMap()
)

fun Routing.analyticsRoutes(analyticsService: AnalyticsService, accessService: ProjectAccessService) {
    route("/api/projects/{projectId}/analytics") {
        // Public endpoint for SDK to send events
        post("/events") {
            val projectIdStr = call.parameters["projectId"] ?: run {
                call.respond(HttpStatusCode.BadRequest, "Project ID required")
                return@post
            }
            val projectId = try {
                UUID.fromString(projectIdStr)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid project ID")
                return@post
            }
            
            val request = try {
                call.receive<AnalyticsEventRequest>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                return@post
            }
            
            val userId = call.getUserId()
            
            val event = AnalyticsEvent(
                projectId = projectIdStr,
                eventType = request.eventType,
                entityType = request.entityType,
                entityId = request.entityId,
                userId = userId,
                attributes = request.attributes,
                timestamp = System.currentTimeMillis()
            )
            
            analyticsService.recordEvent(event)
            call.respond(HttpStatusCode.Created, mapOf("status" to "recorded"))
        }
        
        // Admin endpoints (require auth)
        route("/admin") {
            get("/flags") {
                val projectIdStr = call.parameters["projectId"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Project ID required")
                    return@get
                }
                val projectId = try {
                    UUID.fromString(projectIdStr)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid project ID")
                    return@get
                }
                
                val userId = call.getUserId()?.let { UUID.fromString(it) }
                if (userId == null || !accessService.checkProjectAccess(userId, projectId)) {
                    call.respond(HttpStatusCode.Forbidden, "Access denied")
                    return@get
                }
                
                val stats = analyticsService.getFlagStats(projectId)
                call.respond(HttpStatusCode.OK, stats)
            }
            
            get("/experiments") {
                val projectIdStr = call.parameters["projectId"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Project ID required")
                    return@get
                }
                val projectId = try {
                    UUID.fromString(projectIdStr)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid project ID")
                    return@get
                }
                
                val userId = call.getUserId()?.let { UUID.fromString(it) }
                if (userId == null || !accessService.checkProjectAccess(userId, projectId)) {
                    call.respond(HttpStatusCode.Forbidden, "Access denied")
                    return@get
                }
                
                val stats = analyticsService.getExperimentStats(projectId)
                call.respond(HttpStatusCode.OK, stats)
            }
            
            get("/overview") {
                val projectIdStr = call.parameters["projectId"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Project ID required")
                    return@get
                }
                val projectId = try {
                    UUID.fromString(projectIdStr)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid project ID")
                    return@get
                }
                
                val userId = call.getUserId()?.let { UUID.fromString(it) }
                if (userId == null || !accessService.checkProjectAccess(userId, projectId)) {
                    call.respond(HttpStatusCode.Forbidden, "Access denied")
                    return@get
                }
                
                val period = call.request.queryParameters["period"] ?: "24h"
                val overview = analyticsService.getOverview(projectId, period)
                call.respond(HttpStatusCode.OK, overview)
            }
        }
    }
}

