package io.maxluxs.flagship.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.maxluxs.flagship.shared.api.RestExperiment
import io.maxluxs.flagship.shared.api.RestFlagValue
import io.maxluxs.flagship.shared.api.FlagResponse
import io.maxluxs.flagship.shared.api.CreateExperimentRequest
import io.maxluxs.flagship.shared.api.UpdateExperimentRequest
import io.maxluxs.flagship.shared.api.ExperimentResponse
import io.maxluxs.flagship.server.Storage
import io.maxluxs.flagship.server.auth.getUserId
import io.maxluxs.flagship.server.auth.ProjectAccessService
import io.maxluxs.flagship.server.auth.ApiKeyService
import io.maxluxs.flagship.server.auth.getApiKey
import io.maxluxs.flagship.server.audit.AuditService
import io.maxluxs.flagship.server.database.models.AuditAction
import io.maxluxs.flagship.server.database.models.ProjectRole
import io.maxluxs.flagship.server.util.ValidationUtils
import java.util.*

fun Routing.projectRoutes(storage: Storage, auditService: AuditService, accessService: ProjectAccessService, apiKeyService: ApiKeyService = ApiKeyService()) {
    authenticate("jwt-auth") {
        route("/api/projects/{projectId}") {
            route("/flags") {
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
                    
                    try {
                        val flags = storage.getAllFlags(projectId)
                        call.respond(flags)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to retrieve flags: ${e.message}")
                        )
                    }
                }
                
                get("detailed") {
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
                    
                    try {
                        val flags = storage.getAllFlagsDetailed(projectId)
                        call.respond(flags)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to retrieve flags: ${e.message}")
                        )
                    }
                }
                
                get("{key}") {
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
                    
                    val key = call.parameters["key"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing flag key")
                    )
                    
                    if (!ValidationUtils.validateFlagKey(key)) {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid flag key format. Only letters, numbers, underscores, and hyphens are allowed")
                        )
                    }
                    
                    try {
                        val flag = storage.getFlag(projectId, key)
                        if (flag != null) {
                            call.respond(mapOf(key to flag))
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Flag not found"))
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to retrieve flag: ${e.message}")
                        )
                    }
                }
                
                post {
                    val projectIdParam = call.parameters["projectId"] 
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    
                    if (!ValidationUtils.validateUUID(projectIdParam)) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid projectId format")
                        )
                    }
                    
                    val projectId = UUID.fromString(projectIdParam)
                    val userId = UUID.fromString(call.getUserId() ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Unauthorized")
                    ))
                    
                    if (!accessService.checkProjectAccess(userId, projectId)) {
                        return@post call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }
                    
                    if (!accessService.checkProjectRole(userId, projectId, ProjectRole.ADMIN)) {
                        return@post call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Insufficient permissions. ADMIN role or higher required")
                        )
                    }
                    
                    try {
                        val body = call.receive<Map<String, RestFlagValue>>()
                        if (body.size != 1) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Expected exactly one flag key-value pair")
                            )
                        }
                        
                        val (key, flag) = body.entries.first()
                        
                        if (!ValidationUtils.validateFlagKey(key)) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Invalid flag key format. Only letters, numbers, underscores, and hyphens are allowed")
                            )
                        }
                        
                        val flagValidation = ValidationUtils.validateRestFlagValue(flag)
                        if (!flagValidation.isValid) {
                            val errorMessage = flagValidation.error ?: "Invalid flag value"
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to errorMessage)
                            )
                        }
                        
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
                        val errorMessage = e.message ?: "Conflict"
                        call.respond(HttpStatusCode.Conflict, mapOf("error" to errorMessage))
                    } catch (e: kotlinx.serialization.SerializationException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid request body format: ${e.message}")
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to create flag: ${e.message}")
                        )
                    }
                }
                
                put("{key}") {
                    val projectIdParam = call.parameters["projectId"] 
                        ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    
                    if (!ValidationUtils.validateUUID(projectIdParam)) {
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid projectId format")
                        )
                    }
                    
                    val projectId = UUID.fromString(projectIdParam)
                    val userId = UUID.fromString(call.getUserId() ?: return@put call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Unauthorized")
                    ))
                    
                    if (!accessService.checkProjectAccess(userId, projectId)) {
                        return@put call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }
                    
                    if (!accessService.checkProjectRole(userId, projectId, ProjectRole.ADMIN)) {
                        return@put call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Insufficient permissions. ADMIN role or higher required")
                        )
                    }
                    
                    val key = call.parameters["key"] ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing flag key")
                    )
                    
                    if (!ValidationUtils.validateFlagKey(key)) {
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid flag key format. Only letters, numbers, underscores, and hyphens are allowed")
                        )
                    }
                    
                    try {
                        val flag = call.receive<RestFlagValue>()
                        
                        val flagValidation = ValidationUtils.validateRestFlagValue(flag)
                        if (!flagValidation.isValid) {
                            val errorMessage = flagValidation.error ?: "Invalid flag value"
                            return@put call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to errorMessage)
                            )
                        }
                        
                        val updated = storage.updateFlag(projectId, key, flag)
                        if (updated != null) {
                            auditService.log(
                                AuditAction.FLAG_UPDATED,
                                "flag",
                                key,
                                projectId,
                                userId,
                                mapOf("key" to key, "type" to flag.type),
                                call
                            )
                            call.respond(mapOf(key to updated))
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Flag not found"))
                        }
                    } catch (e: kotlinx.serialization.SerializationException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid request body format: ${e.message}")
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to update flag: ${e.message}")
                        )
                    }
                }
                
                patch("{key}/toggle") {
                    val projectIdParam = call.parameters["projectId"] 
                        ?: return@patch call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    
                    if (!ValidationUtils.validateUUID(projectIdParam)) {
                        return@patch call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid projectId format")
                        )
                    }
                    
                    val projectId = UUID.fromString(projectIdParam)
                    val userId = UUID.fromString(call.getUserId() ?: return@patch call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Unauthorized")
                    ))
                    
                    if (!accessService.checkProjectAccess(userId, projectId)) {
                        return@patch call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }
                    
                    if (!accessService.checkProjectRole(userId, projectId, ProjectRole.ADMIN)) {
                        return@patch call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Insufficient permissions. ADMIN role or higher required")
                        )
                    }
                    
                    val key = call.parameters["key"] ?: return@patch call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing flag key")
                    )
                    
                    if (!ValidationUtils.validateFlagKey(key)) {
                        return@patch call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid flag key format. Only letters, numbers, underscores, and hyphens are allowed")
                        )
                    }
                    
                    try {
                        val toggled = storage.toggleFlag(projectId, key)
                        if (toggled != null) {
                            auditService.log(
                                AuditAction.FLAG_UPDATED,
                                "flag",
                                key,
                                projectId,
                                userId,
                                mapOf("key" to key, "isEnabled" to toggled.isEnabled),
                                call
                            )
                            call.respond(toggled)
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Flag not found"))
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to toggle flag: ${e.message}")
                        )
                    }
                }
                
                delete("{key}") {
                    val projectIdParam = call.parameters["projectId"] 
                        ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    
                    if (!ValidationUtils.validateUUID(projectIdParam)) {
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid projectId format")
                        )
                    }
                    
                    val projectId = UUID.fromString(projectIdParam)
                    val userId = UUID.fromString(call.getUserId() ?: return@delete call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Unauthorized")
                    ))
                    
                    if (!accessService.checkProjectAccess(userId, projectId)) {
                        return@delete call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }
                    
                    if (!accessService.checkProjectRole(userId, projectId, ProjectRole.ADMIN)) {
                        return@delete call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Insufficient permissions. ADMIN role or higher required")
                        )
                    }
                    
                    val key = call.parameters["key"] ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing flag key")
                    )
                    
                    if (!ValidationUtils.validateFlagKey(key)) {
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid flag key format. Only letters, numbers, underscores, and hyphens are allowed")
                        )
                    }
                    
                    try {
                        val deleted = storage.deleteFlag(projectId, key)
                        if (deleted) {
                            auditService.log(
                                AuditAction.FLAG_DELETED,
                                "flag",
                                key,
                                projectId,
                                userId,
                                null,
                                call
                            )
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Flag not found"))
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to delete flag: ${e.message}")
                        )
                    }
                }
            }
            
            route("/experiments") {
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
                    
                    try {
                        val experiments = storage.getAllExperiments(projectId)
                        call.respond(experiments)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to retrieve experiments: ${e.message}")
                        )
                    }
                }
                
                get("detailed") {
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
                    
                    try {
                        val experiments = storage.getAllExperiments(projectId)
                        val responses = experiments.map { (key, experiment) ->
                            val metadata = storage.getExperimentMetadata(projectId, key)
                            ExperimentResponse(
                                key = key,
                                experiment = experiment,
                                description = null, // Description not stored yet
                                isActive = metadata?.isActive ?: true,
                                createdAt = metadata?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = metadata?.updatedAt ?: System.currentTimeMillis()
                            )
                        }
                        call.respond(responses)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to retrieve experiments: ${e.message}")
                        )
                    }
                }
                
                get("{key}") {
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
                    
                    val key = call.parameters["key"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing experiment key")
                    )
                    
                    if (!ValidationUtils.validateExperimentKey(key)) {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid experiment key format. Only letters, numbers, underscores, and hyphens are allowed")
                        )
                    }
                    
                    try {
                        val experiment = storage.getExperiment(projectId, key)
                        if (experiment != null) {
                            val metadata = storage.getExperimentMetadata(projectId, key)
                            val response = ExperimentResponse(
                                key = key,
                                experiment = experiment,
                                description = null, // Description not stored yet
                                isActive = metadata?.isActive ?: true,
                                createdAt = metadata?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = metadata?.updatedAt ?: System.currentTimeMillis()
                            )
                            call.respond(response)
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Experiment not found"))
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to retrieve experiment: ${e.message}")
                        )
                    }
                }
                
                post {
                    val projectIdParam = call.parameters["projectId"] 
                        ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    
                    if (!ValidationUtils.validateUUID(projectIdParam)) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid projectId format")
                        )
                    }
                    
                    val projectId = UUID.fromString(projectIdParam)
                    val userId = UUID.fromString(call.getUserId() ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Unauthorized")
                    ))
                    
                    if (!accessService.checkProjectAccess(userId, projectId)) {
                        return@post call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }
                    
                    if (!accessService.checkProjectRole(userId, projectId, ProjectRole.ADMIN)) {
                        return@post call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Insufficient permissions. ADMIN role or higher required")
                        )
                    }
                    
                    try {
                        // Support both old format (Map<String, RestExperiment>) and new format (CreateExperimentRequest)
                        // Try new format first, fallback to old format for backward compatibility
                        val key: String
                        val request: CreateExperimentRequest
                        
                        val keyFromUrl = call.parameters["key"]
                        if (keyFromUrl != null) {
                            // New format: key in URL
                            request = call.receive<CreateExperimentRequest>()
                            key = keyFromUrl
                        } else {
                            // Old format: key in body (backward compatibility)
                            try {
                                val oldBody = call.receive<Map<String, RestExperiment>>()
                                if (oldBody.size != 1) {
                                    return@post call.respond(
                                        HttpStatusCode.BadRequest,
                                        mapOf("error" to "Expected exactly one experiment key-value pair")
                                    )
                                }
                                val (oldKey, oldExperiment) = oldBody.entries.first()
                                key = oldKey
                                request = CreateExperimentRequest(
                                    experiment = oldExperiment,
                                    description = null,
                                    isActive = true
                                )
                            } catch (e: kotlinx.serialization.SerializationException) {
                                // If old format fails, return error
                                return@post call.respond(
                                    HttpStatusCode.BadRequest,
                                    mapOf("error" to "Invalid request format. Expected either Map<String, RestExperiment> (old format) or CreateExperimentRequest with key in URL (new format)")
                                )
                            }
                        }
                        
                        if (!ValidationUtils.validateExperimentKey(key)) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Invalid experiment key format. Only letters, numbers, underscores, and hyphens are allowed")
                            )
                        }
                        
                        val experiment = request.experiment
                        val experimentValidation = ValidationUtils.validateRestExperiment(experiment)
                        if (!experimentValidation.isValid) {
                            val errorMessage = experimentValidation.error ?: "Invalid experiment data"
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to errorMessage)
                            )
                        }
                        
                        val created = storage.createExperiment(projectId, key, experiment, userId)
                        val metadata = storage.getExperimentMetadata(projectId, key)
                        
                        auditService.log(
                            AuditAction.EXPERIMENT_CREATED,
                            "experiment",
                            key,
                            projectId,
                            userId,
                            mapOf("key" to key, "variants" to experiment.variants.size),
                            call
                        )
                        
                        val response = ExperimentResponse(
                            key = key,
                            experiment = created,
                            description = request.description,
                            isActive = request.isActive,
                            createdAt = metadata?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = metadata?.updatedAt ?: System.currentTimeMillis()
                        )
                        call.respond(HttpStatusCode.Created, response)
                    } catch (e: IllegalArgumentException) {
                        val errorMessage = e.message ?: "Conflict"
                        call.respond(HttpStatusCode.Conflict, mapOf("error" to errorMessage))
                    } catch (e: kotlinx.serialization.SerializationException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid request body format: ${e.message}")
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to create experiment: ${e.message}")
                        )
                    }
                }
                
                put("{key}") {
                    val projectIdParam = call.parameters["projectId"] 
                        ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    
                    if (!ValidationUtils.validateUUID(projectIdParam)) {
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid projectId format")
                        )
                    }
                    
                    val projectId = UUID.fromString(projectIdParam)
                    val userId = UUID.fromString(call.getUserId() ?: return@put call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Unauthorized")
                    ))
                    
                    if (!accessService.checkProjectAccess(userId, projectId)) {
                        return@put call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }
                    
                    if (!accessService.checkProjectRole(userId, projectId, ProjectRole.ADMIN)) {
                        return@put call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Insufficient permissions. ADMIN role or higher required")
                        )
                    }
                    
                    val key = call.parameters["key"] ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing experiment key")
                    )
                    
                    if (!ValidationUtils.validateExperimentKey(key)) {
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid experiment key format. Only letters, numbers, underscores, and hyphens are allowed")
                        )
                    }
                    
                    try {
                        val request = call.receive<UpdateExperimentRequest>()
                        val experiment = request.experiment
                        
                        val experimentValidation = ValidationUtils.validateRestExperiment(experiment)
                        if (!experimentValidation.isValid) {
                            val errorMessage = experimentValidation.error ?: "Invalid experiment data"
                            return@put call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to errorMessage)
                            )
                        }
                        
                        val updated = storage.updateExperiment(projectId, key, experiment)
                        if (updated != null) {
                            val metadata = storage.getExperimentMetadata(projectId, key)
                            
                            auditService.log(
                                AuditAction.EXPERIMENT_UPDATED,
                                "experiment",
                                key,
                                projectId,
                                userId,
                                mapOf("key" to key, "variants" to experiment.variants.size),
                                call
                            )
                            
                            val response = ExperimentResponse(
                                key = key,
                                experiment = updated,
                                description = request.description,
                                isActive = request.isActive ?: (metadata?.isActive ?: true),
                                createdAt = metadata?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = metadata?.updatedAt ?: System.currentTimeMillis()
                            )
                            call.respond(response)
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Experiment not found"))
                        }
                    } catch (e: kotlinx.serialization.SerializationException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid request body format: ${e.message}")
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to update experiment: ${e.message}")
                        )
                    }
                }
                
                delete("{key}") {
                    val projectIdParam = call.parameters["projectId"] 
                        ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    
                    if (!ValidationUtils.validateUUID(projectIdParam)) {
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid projectId format")
                        )
                    }
                    
                    val projectId = UUID.fromString(projectIdParam)
                    val userId = UUID.fromString(call.getUserId() ?: return@delete call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Unauthorized")
                    ))
                    
                    if (!accessService.checkProjectAccess(userId, projectId)) {
                        return@delete call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }
                    
                    if (!accessService.checkProjectRole(userId, projectId, ProjectRole.ADMIN)) {
                        return@delete call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Insufficient permissions. ADMIN role or higher required")
                        )
                    }
                    
                    val key = call.parameters["key"] ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing experiment key")
                    )
                    
                    if (!ValidationUtils.validateExperimentKey(key)) {
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid experiment key format. Only letters, numbers, underscores, and hyphens are allowed")
                        )
                    }
                    
                    try {
                        val deleted = storage.deleteExperiment(projectId, key)
                        if (deleted) {
                            auditService.log(
                                AuditAction.EXPERIMENT_DELETED,
                                "experiment",
                                key,
                                projectId,
                                userId,
                                null,
                                call
                            )
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Experiment not found"))
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to delete experiment: ${e.message}")
                        )
                    }
                }
            }
            
            route("/config") {
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
                    
                    // Check authentication - can be JWT or API key
                    val userId = call.getUserId()?.let { UUID.fromString(it) }
                    val apiKey = call.getApiKey()
                    
                    var hasAccess = false
                    
                    if (userId != null) {
                        hasAccess = accessService.checkProjectAccess(userId, projectId)
                    } else if (apiKey != null) {
                        hasAccess = apiKeyService.checkApiKeyAccess(apiKey, projectId, null)
                    }
                    
                    if (!hasAccess) {
                        return@get call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }
                    
                    try {
                        val revision = call.request.queryParameters["rev"]
                        val config = storage.getConfig(projectId, revision)
                        call.respond(config)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to retrieve config: ${e.message}")
                        )
                    }
                }
            }
        }
    }
}

