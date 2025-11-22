package io.maxluxs.flagship.server.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.maxluxs.flagship.server.audit.AuditService
import io.maxluxs.flagship.server.auth.getUserId
import java.util.*

fun Routing.auditRoutes(auditService: AuditService) {
    authenticate("jwt-auth") {
        route("/api/admin") {
            route("/projects/{projectId}/audit") {
                get {
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                    val logs = auditService.getLogs(projectId = projectId, limit = limit)
                    call.respond(logs)
                }
            }
        }
    }
}

