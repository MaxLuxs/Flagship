package io.maxluxs.flagship.server.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.maxluxs.flagship.server.audit.AuditService
import io.maxluxs.flagship.server.auth.getUserId
import io.maxluxs.flagship.server.auth.ProjectAccessService
import io.maxluxs.flagship.server.database.models.AuditAction
import io.maxluxs.flagship.server.util.ValidationUtils
import java.util.*

fun Routing.auditRoutes(auditService: AuditService, accessService: ProjectAccessService) {
    authenticate("jwt-auth") {
        route("/api/admin") {
            route("/projects/{projectId}/audit") {
                get {
                    val projectIdParam = call.parameters["projectId"] 
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    
                    if (!ValidationUtils.validateUUID(projectIdParam)) {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid projectId format")
                        )
                    }
                    
                    val projectId = UUID.fromString(projectIdParam)
                    val userId = UUID.fromString(call.getUserId() ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Unauthorized")
                    ))
                    
                    if (!accessService.checkProjectAccess(userId, projectId)) {
                        return@get call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }
                    
                    val limitParam = call.request.queryParameters["limit"]?.toIntOrNull()
                    val limit = when {
                        limitParam == null -> 100
                        limitParam < 1 -> return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Limit must be greater than 0")
                        )
                        limitParam > 1000 -> return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Limit cannot exceed 1000")
                        )
                        else -> limitParam
                    }
                    
                    val offsetParam = call.request.queryParameters["offset"]?.toIntOrNull()
                    val offset = when {
                        offsetParam == null -> 0
                        offsetParam < 0 -> return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Offset must be greater than or equal to 0")
                        )
                        else -> offsetParam
                    }
                    
                    val actionTypeParam = call.request.queryParameters["action"]
                    val actionType = actionTypeParam?.let {
                        try {
                            AuditAction.valueOf(it.uppercase())
                        } catch (e: IllegalArgumentException) {
                            return@get call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Invalid action type: $it")
                            )
                        }
                    }
                    
                    try {
                        val logs = auditService.getLogs(
                            projectId = projectId, 
                            limit = limit,
                            offset = offset,
                            actionType = actionType
                        )
                        call.respond(logs)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to retrieve audit logs: ${e.message}")
                        )
                    }
                }
            }
        }
    }
}

