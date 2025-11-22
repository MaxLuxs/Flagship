package io.maxluxs.flagship.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.maxluxs.flagship.provider.rest.RestExperiment
import io.maxluxs.flagship.provider.rest.RestFlagValue
import io.maxluxs.flagship.server.Storage
import io.maxluxs.flagship.server.auth.getUserId
import io.maxluxs.flagship.server.audit.AuditService
import io.maxluxs.flagship.server.database.models.AuditAction
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class FlagResponse(
    val key: String,
    val type: String,
    val value: String,
    val description: String?,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

fun Routing.projectRoutes(storage: Storage, auditService: AuditService) {
    authenticate("jwt-auth") {
        route("/api/projects/{projectId}") {
            route("/flags") {
                get {
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    val flags = storage.getAllFlags(projectId)
                    call.respond(flags)
                }
                
                get("{key}") {
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    val key = call.parameters["key"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing flag key")
                    )
                    
                    val flag = storage.getFlag(projectId, key)
                    if (flag != null) {
                        call.respond(mapOf(key to flag))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Flag not found"))
                    }
                }
                
                post {
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    val userId = UUID.fromString(call.getUserId() ?: "")
                    
                    try {
                        val body = call.receive<Map<String, RestFlagValue>>()
                        if (body.size != 1) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Expected exactly one flag key-value pair")
                            )
                            return@post
                        }
                        
                        val (key, flag) = body.entries.first()
                        val created = storage.createFlag(projectId, key, flag, userId)
                        auditService.log(
                            AuditAction.FLAG_CREATED,
                            "flag",
                            key,
                            projectId,
                            userId,
                            mapOf("key" to key, "type" to flag.type),
                            call
                        )
                        call.respond(HttpStatusCode.Created, mapOf(key to created))
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.Conflict, mapOf("error" to (e.message ?: "Conflict")))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid request body: ${e.message}")
                        )
                    }
                }
                
                put("{key}") {
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    val key = call.parameters["key"] ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing flag key")
                    )
                    
                    try {
                        val flag = call.receive<RestFlagValue>()
                        val updated = storage.updateFlag(projectId, key, flag)
                        if (updated != null) {
                            auditService.log(
                                AuditAction.FLAG_UPDATED,
                                "flag",
                                key,
                                projectId,
                                UUID.fromString(call.getUserId() ?: ""),
                                mapOf("key" to key, "type" to flag.type),
                                call
                            )
                            call.respond(mapOf(key to updated))
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Flag not found"))
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid request body: ${e.message}")
                        )
                    }
                }
                
                delete("{key}") {
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    val key = call.parameters["key"] ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing flag key")
                    )
                    
                    val deleted = storage.deleteFlag(projectId, key)
                    if (deleted) {
                        auditService.log(
                            AuditAction.FLAG_DELETED,
                            "flag",
                            key,
                            projectId,
                            UUID.fromString(call.getUserId() ?: ""),
                            null,
                            call
                        )
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Flag not found"))
                    }
                }
            }
            
            route("/experiments") {
                get {
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    val experiments = storage.getAllExperiments(projectId)
                    call.respond(experiments)
                }
                
                get("{key}") {
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    val key = call.parameters["key"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing experiment key")
                    )
                    
                    val experiment = storage.getExperiment(projectId, key)
                    if (experiment != null) {
                        call.respond(mapOf(key to experiment))
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Experiment not found"))
                    }
                }
                
                post {
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    val userId = UUID.fromString(call.getUserId() ?: "")
                    
                    try {
                        val body = call.receive<Map<String, RestExperiment>>()
                        if (body.size != 1) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Expected exactly one experiment key-value pair")
                            )
                            return@post
                        }
                        
                        val (key, experiment) = body.entries.first()
                        val created = storage.createExperiment(projectId, key, experiment, userId)
                        auditService.log(
                            AuditAction.EXPERIMENT_CREATED,
                            "experiment",
                            key,
                            projectId,
                            userId,
                            mapOf("key" to key, "variants" to experiment.variants.size),
                            call
                        )
                        call.respond(HttpStatusCode.Created, mapOf(key to created))
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.Conflict, mapOf("error" to (e.message ?: "Conflict")))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid request body: ${e.message}")
                        )
                    }
                }
                
                put("{key}") {
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    val key = call.parameters["key"] ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing experiment key")
                    )
                    
                    try {
                        val experiment = call.receive<RestExperiment>()
                        val updated = storage.updateExperiment(projectId, key, experiment)
                        if (updated != null) {
                            auditService.log(
                                AuditAction.EXPERIMENT_UPDATED,
                                "experiment",
                                key,
                                projectId,
                                UUID.fromString(call.getUserId() ?: ""),
                                mapOf("key" to key, "variants" to experiment.variants.size),
                                call
                            )
                            call.respond(mapOf(key to updated))
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Experiment not found"))
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid request body: ${e.message}")
                        )
                    }
                }
                
                delete("{key}") {
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    val key = call.parameters["key"] ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing experiment key")
                    )
                    
                    val deleted = storage.deleteExperiment(projectId, key)
                    if (deleted) {
                        auditService.log(
                            AuditAction.EXPERIMENT_DELETED,
                            "experiment",
                            key,
                            projectId,
                            UUID.fromString(call.getUserId() ?: ""),
                            null,
                            call
                        )
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Experiment not found"))
                    }
                }
            }
            
            route("/config") {
                get {
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    val revision = call.request.queryParameters["rev"]
                    val config = storage.getConfig(projectId, revision)
                    call.respond(config)
                }
            }
        }
    }
}

