package io.maxluxs.flagship.server.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.maxluxs.flagship.server.auth.getUserId
import io.maxluxs.flagship.server.auth.requireAuth
import io.maxluxs.flagship.server.audit.AuditService
import io.maxluxs.flagship.server.database.models.AuditAction
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.datetime.Clock
import java.util.*
import io.maxluxs.flagship.server.database.models.*

@Serializable
data class CreateProjectRequest(
    val name: String,
    val slug: String,
    val description: String? = null
)

@Serializable
data class ProjectResponse(
    val id: String,
    val name: String,
    val slug: String,
    val description: String?,
    val ownerId: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CreateApiKeyRequest(
    val name: String,
    val type: String // "READ_ONLY" or "ADMIN"
)

@Serializable
data class ApiKeyResponse(
    val id: String,
    val name: String,
    val key: String, // Only shown once on creation
    val type: String,
    val createdAt: Long,
    val expiresAt: Long?
)

fun Routing.adminRoutes(auditService: AuditService) {
    authenticate("jwt-auth") {
        route("/api/admin") {
            route("/projects") {
                get {
                    val userId = UUID.fromString(call.getUserId() ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Unauthorized")
                    ))
                    
                    val projects = transaction {
                        (Projects innerJoin ProjectMembers)
                            .select { ProjectMembers.userId eq userId }
                            .map { row ->
                                ProjectResponse(
                                    id = row[Projects.id].value.toString(),
                                    name = row[Projects.name],
                                    slug = row[Projects.slug],
                                    description = row[Projects.description],
                                    ownerId = row[Projects.ownerId].toString(),
                                    createdAt = row[Projects.createdAt].epochSeconds,
                                    updatedAt = row[Projects.updatedAt].epochSeconds
                                )
                            }
                    }
                    
                    call.respond(projects)
                }
                
                post {
                    val userId = UUID.fromString(call.getUserId() ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Unauthorized")
                    ))
                    
                    try {
                        val request = call.receive<CreateProjectRequest>()
                        
                        val project = transaction {
                            val now = kotlinx.datetime.Clock.System.now()
                            val projectId = Projects.insert {
                                it[Projects.name] = request.name
                                it[Projects.slug] = request.slug
                                it[Projects.description] = request.description
                                it[Projects.ownerId] = userId
                                it[Projects.createdAt] = now
                                it[Projects.updatedAt] = now
                            } get Projects.id
                            
                            ProjectMembers.insert {
                                it[ProjectMembers.projectId] = projectId.value
                                it[ProjectMembers.userId] = userId
                                it[ProjectMembers.role] = ProjectRole.OWNER
                            }
                            
                            Projects.select { Projects.id eq projectId.value }.first().let { row ->
                                ProjectResponse(
                                    id = row[Projects.id].value.toString(),
                                    name = row[Projects.name],
                                    slug = row[Projects.slug],
                                    description = row[Projects.description],
                                    ownerId = row[Projects.ownerId].toString(),
                                    createdAt = row[Projects.createdAt].epochSeconds,
                                    updatedAt = row[Projects.updatedAt].epochSeconds
                                )
                            }
                        }
                        
                        auditService.log(
                            AuditAction.PROJECT_CREATED,
                            "project",
                            project.id,
                            UUID.fromString(project.id),
                            userId,
                            mapOf("name" to project.name, "slug" to project.slug),
                            call
                        )
                        
                        call.respond(HttpStatusCode.Created, project)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to create project: ${e.message}")
                        )
                    }
                }
                
                get("{projectId}") {
                    val userId = UUID.fromString(call.getUserId() ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Unauthorized")
                    ))
                    
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    val project = transaction {
                        (Projects innerJoin ProjectMembers)
                            .select { 
                                (Projects.id eq projectId) and
                                (ProjectMembers.userId eq userId)
                            }
                            .firstOrNull()?.let { row ->
                                ProjectResponse(
                                    id = row[Projects.id].value.toString(),
                                    name = row[Projects.name],
                                    slug = row[Projects.slug],
                                    description = row[Projects.description],
                                    ownerId = row[Projects.ownerId].toString(),
                                    createdAt = row[Projects.createdAt].epochSeconds,
                                    updatedAt = row[Projects.updatedAt].epochSeconds
                                )
                            }
                    }
                    
                    if (project == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Project not found"))
                    } else {
                        call.respond(project)
                    }
                }
            }
            
            route("/projects/{projectId}/api-keys") {
                get {
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    val keys = transaction {
                        ApiKeys.select { ApiKeys.projectId eq projectId }
                            .map { row ->
                                ApiKeyResponse(
                                    id = row[ApiKeys.id].value.toString(),
                                    name = row[ApiKeys.name],
                                    key = "***", // Never show full key
                                    type = row[ApiKeys.type].name,
                                    createdAt = row[ApiKeys.createdAt].epochSeconds,
                                    expiresAt = row[ApiKeys.expiresAt]?.epochSeconds
                                )
                            }
                    }
                    
                    call.respond(keys)
                }
                
                post {
                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )
                    
                    try {
                        val request = call.receive<CreateApiKeyRequest>()
                        val keyType = ApiKeyType.valueOf(request.type)
                        
                        val apiKey = "sk_${UUID.randomUUID().toString().replace("-", "")}"
                        val keyHash = org.mindrot.jbcrypt.BCrypt.hashpw(apiKey, org.mindrot.jbcrypt.BCrypt.gensalt())
                        
                        val keyResponse = transaction {
                            ApiKeys.insert {
                                it[ApiKeys.projectId] = projectId
                                it[ApiKeys.keyHash] = keyHash
                                it[ApiKeys.name] = request.name
                                it[ApiKeys.type] = keyType
                            } get ApiKeys.id
                            
                            ApiKeyResponse(
                                id = keyHash, // Use hash as ID for lookup
                                name = request.name,
                                key = apiKey, // Show only once
                                type = request.type,
                                createdAt = System.currentTimeMillis() / 1000,
                                expiresAt = null
                            )
                        }
                        
                        auditService.log(
                            AuditAction.API_KEY_CREATED,
                            "api_key",
                            keyResponse.id,
                            projectId,
                            UUID.fromString(call.getUserId() ?: ""),
                            mapOf("name" to request.name, "type" to request.type),
                            call
                        )
                        
                        call.respond(HttpStatusCode.Created, keyResponse)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to create API key: ${e.message}")
                        )
                    }
                }
            }
        }
    }
}

