@file:OptIn(ExperimentalTime::class)

package io.maxluxs.flagship.server.admin

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.maxluxs.flagship.server.audit.AuditService
import io.maxluxs.flagship.server.auth.ProjectAccessService
import io.maxluxs.flagship.server.auth.getUserId
import io.maxluxs.flagship.server.database.models.ApiKeyType
import io.maxluxs.flagship.server.database.models.ApiKeys
import io.maxluxs.flagship.server.database.models.AuditAction
import io.maxluxs.flagship.server.database.models.ProjectMembers
import io.maxluxs.flagship.server.database.models.ProjectRole
import io.maxluxs.flagship.server.database.models.Projects
import io.maxluxs.flagship.server.database.models.Users
import io.maxluxs.flagship.server.util.ValidationUtils
import io.maxluxs.flagship.shared.api.ApiKeyResponse
import io.maxluxs.flagship.shared.api.CreateApiKeyRequest
import io.maxluxs.flagship.shared.api.CreateProjectRequest
import io.maxluxs.flagship.shared.api.ProjectResponse
import io.maxluxs.flagship.shared.api.UpdateProjectRequest
import io.maxluxs.flagship.shared.api.ProjectMemberResponse
import io.maxluxs.flagship.shared.api.AddProjectMemberRequest
import kotlinx.serialization.SerializationException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

fun Routing.adminRoutes(auditService: AuditService, accessService: ProjectAccessService) {
    authenticate("jwt-auth") {
        route("/api/admin") {
            route("/projects") {
                get {
                    val userId = UUID.fromString(
                        call.getUserId() ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "Unauthorized")
                        )
                    )

                    val projects = transaction {
                        (Projects innerJoin ProjectMembers)
                            .selectAll().where { ProjectMembers.userId eq userId }
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
                    val userId = UUID.fromString(
                        call.getUserId() ?: return@post call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "Unauthorized")
                        )
                    )

                    try {
                        val request = call.receive<CreateProjectRequest>()

                        // Validate name
                        if (request.name.isBlank() || request.name.length > 100) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Project name must be between 1 and 100 characters")
                            )
                        }

                        // Validate slug
                        if (!ValidationUtils.validateSlug(request.slug)) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Invalid slug format. Only lowercase letters, numbers, and hyphens are allowed")
                            )
                        }

                        // Check slug uniqueness
                        val existingSlug = transaction {
                            Projects.selectAll().where { Projects.slug eq request.slug }
                                .firstOrNull()
                        }
                        if (existingSlug != null) {
                            return@post call.respond(
                                HttpStatusCode.Conflict,
                                mapOf("error" to "Project with this slug already exists")
                            )
                        }

                        val project = transaction {
                            val now = Clock.System.now()
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
                                it[ProjectMembers.joinedAt] = Clock.System.now()
                            }

                            Projects.selectAll().where { Projects.id eq projectId.value }
                                .first().let { row ->
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
                    } catch (e: ExposedSQLException) {
                        if (e.message?.contains("unique") == true || e.message?.contains("duplicate") == true) {
                            call.respond(
                                HttpStatusCode.Conflict,
                                mapOf("error" to "Project with this slug already exists")
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                mapOf("error" to "Failed to create project: ${e.message}")
                            )
                        }
                    } catch (e: SerializationException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid request body format: ${e.message}")
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Failed to create project: ${e.message}")
                        )
                    }
                }

                get("{projectId}") {
                    val userId = UUID.fromString(
                        call.getUserId() ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "Unauthorized")
                        )
                    )

                    val projectId = UUID.fromString(
                        call.parameters["projectId"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )
                    )

                    val project = transaction {
                        (Projects innerJoin ProjectMembers)
                            .selectAll().where {
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
                        if (!accessService.checkProjectAccess(userId, projectId)) {
                            call.respond(
                                HttpStatusCode.Forbidden,
                                mapOf("error" to "Access denied to this project")
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                mapOf("error" to "Project not found")
                            )
                        }
                    } else {
                        call.respond(project)
                    }
                }

                put("{projectId}") {
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
                    val userId = UUID.fromString(
                        call.getUserId() ?: return@put call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "Unauthorized")
                        )
                    )

                    if (!accessService.checkProjectAccess(userId, projectId)) {
                        return@put call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }

                    if (!accessService.checkProjectRole(userId, projectId, ProjectRole.ADMIN)) {
                        return@put call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Insufficient permissions. ADMIN role or higher required to edit project")
                        )
                    }

                    try {
                        val request = call.receive<UpdateProjectRequest>()

                        val nameValue = request.name
                        if (nameValue != null && (nameValue.isBlank() || nameValue.length > 100)) {
                            return@put call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Project name must be between 1 and 100 characters")
                            )
                        }

                        val updatedProject = transaction {
                            val project = Projects.selectAll().where { Projects.id eq projectId }
                                .firstOrNull()
                            if (project == null) {
                                return@transaction null
                            }

                            Projects.update({ Projects.id eq projectId }) {
                                val nameValue = request.name
                                if (nameValue != null) {
                                    it[name] = nameValue
                                }
                                if (request.description != null) {
                                    it[description] = request.description
                                }
                                it[updatedAt] = Clock.System.now()
                            }

                            Projects.selectAll().where { Projects.id eq projectId }
                                .first().let { row ->
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

                        if (updatedProject == null) {
                            return@put call.respond(
                                HttpStatusCode.NotFound,
                                mapOf("error" to "Project not found")
                            )
                        }

                        auditService.log(
                            AuditAction.PROJECT_UPDATED,
                            "project",
                            updatedProject.id,
                            projectId,
                            userId,
                            mapOf(
                                "name" to (request.name ?: "unchanged"),
                                "description" to (request.description ?: "unchanged")
                            ),
                            call
                        )

                        call.respond(updatedProject)
                    } catch (e: SerializationException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid request body format: ${e.message}")
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to update project: ${e.message}")
                        )
                    }
                }

                delete("{projectId}") {
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
                    val userId = UUID.fromString(
                        call.getUserId() ?: return@delete call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "Unauthorized")
                        )
                    )

                    if (!accessService.checkProjectAccess(userId, projectId)) {
                        return@delete call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }

                    val userRole = accessService.getUserProjectRole(userId, projectId)
                    if (userRole != ProjectRole.OWNER) {
                        return@delete call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Only project owner can delete the project")
                        )
                    }

                    val confirmParam = call.request.queryParameters["confirm"]
                    if (confirmParam != "true") {
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Project deletion requires confirmation. Add ?confirm=true to the URL")
                        )
                    }

                    try {
                        val projectName = transaction {
                            val project = Projects.selectAll().where { Projects.id eq projectId }
                                .firstOrNull()
                            if (project == null) {
                                return@transaction null
                            }

                            val name = project[Projects.name]

                            Projects.deleteWhere { Projects.id eq projectId }

                            name
                        }

                        if (projectName == null) {
                            return@delete call.respond(
                                HttpStatusCode.NotFound,
                                mapOf("error" to "Project not found")
                            )
                        }

                        auditService.log(
                            AuditAction.PROJECT_DELETED,
                            "project",
                            projectId.toString(),
                            projectId,
                            userId,
                            mapOf("name" to projectName),
                            call
                        )

                        call.respond(
                            HttpStatusCode.OK,
                            mapOf("message" to "Project deleted successfully")
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to delete project: ${e.message}")
                        )
                    }
                }
            }

            route("/projects/{projectId}/api-keys") {
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
                    val userId = UUID.fromString(
                        call.getUserId() ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "Unauthorized")
                        )
                    )

                    if (!accessService.checkProjectAccess(userId, projectId)) {
                        return@get call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }

                    try {
                        val keys = transaction {
                            ApiKeys.selectAll().where { ApiKeys.projectId eq projectId }
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
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to retrieve API keys: ${e.message}")
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
                    val userId = UUID.fromString(
                        call.getUserId() ?: return@post call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "Unauthorized")
                        )
                    )

                    if (!accessService.checkProjectAccess(userId, projectId)) {
                        return@post call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }

                    if (!accessService.checkProjectRole(userId, projectId, ProjectRole.ADMIN)) {
                        return@post call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Insufficient permissions. ADMIN role or higher required to manage API keys")
                        )
                    }

                    try {
                        val request = call.receive<CreateApiKeyRequest>()

                        if (request.name.isBlank() || request.name.length > 100) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "API key name must be between 1 and 100 characters")
                            )
                        }

                        val validTypes = setOf("READ_ONLY", "ADMIN")
                        if (!validTypes.contains(request.type.uppercase())) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Invalid API key type. Must be one of: ${validTypes.joinToString()}")
                            )
                        }

                        val keyType = try {
                            ApiKeyType.valueOf(request.type.uppercase())
                        } catch (_: IllegalArgumentException) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Invalid API key type: ${request.type}")
                            )
                        }

                        val apiKey = "sk_${UUID.randomUUID().toString().replace("-", "")}"
                        val keyHash = org.mindrot.jbcrypt.BCrypt.hashpw(
                            apiKey,
                            org.mindrot.jbcrypt.BCrypt.gensalt()
                        )

                        val keyResponse = transaction {
                            val now = Clock.System.now()
                            val expiresAt = request.expirationDays?.let { days ->
                                now + days.days
                            }

                            val keyId = ApiKeys.insert {
                                it[ApiKeys.projectId] = projectId
                                it[ApiKeys.keyHash] = keyHash
                                it[ApiKeys.name] = request.name
                                it[ApiKeys.type] = keyType
                                it[ApiKeys.createdAt] = now
                                it[ApiKeys.expiresAt] = expiresAt
                            } get ApiKeys.id

                            ApiKeyResponse(
                                id = keyId.value.toString(),
                                name = request.name,
                                key = apiKey, // Show only once
                                type = request.type.uppercase(),
                                createdAt = now.epochSeconds,
                                expiresAt = expiresAt?.epochSeconds
                            )
                        }

                        auditService.log(
                            AuditAction.API_KEY_CREATED,
                            "api_key",
                            keyResponse.id,
                            projectId,
                            userId,
                            mapOf("name" to request.name, "type" to request.type),
                            call
                        )

                        call.respond(HttpStatusCode.Created, keyResponse)
                    } catch (e: SerializationException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid request body format: ${e.message}")
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to create API key: ${e.message}")
                        )
                    }
                }

                delete("{keyId}") {
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
                    val userId = UUID.fromString(
                        call.getUserId() ?: return@delete call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "Unauthorized")
                        )
                    )

                    if (!accessService.checkProjectAccess(userId, projectId)) {
                        return@delete call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }

                    if (!accessService.checkProjectRole(userId, projectId, ProjectRole.ADMIN)) {
                        return@delete call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Insufficient permissions. ADMIN role or higher required to manage API keys")
                        )
                    }

                    val keyIdParam = call.parameters["keyId"]
                        ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing keyId")
                        )

                    if (!ValidationUtils.validateUUID(keyIdParam)) {
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid keyId format")
                        )
                    }

                    val keyId = UUID.fromString(keyIdParam)

                    try {
                        val deleted = transaction {
                            val key = ApiKeys.selectAll().where {
                                (ApiKeys.id eq keyId) and
                                        (ApiKeys.projectId eq projectId)
                            }.firstOrNull()

                            if (key != null) {
                                ApiKeys.deleteWhere {
                                    (ApiKeys.id eq keyId) and
                                            (ApiKeys.projectId eq projectId)
                                }
                                true
                            } else {
                                false
                            }
                        }

                        if (deleted) {
                            auditService.log(
                                AuditAction.API_KEY_DELETED,
                                "api_key",
                                keyIdParam,
                                projectId,
                                userId,
                                null,
                                call
                            )
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                mapOf("error" to "API key not found")
                            )
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to delete API key: ${e.message}")
                        )
                    }
                }
            }

            route("/projects/{projectId}/members") {
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
                    val userId = UUID.fromString(
                        call.getUserId() ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "Unauthorized")
                        )
                    )

                    if (!accessService.checkProjectAccess(userId, projectId)) {
                        return@get call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Access denied to this project")
                        )
                    }

                    try {
                        val members = transaction {
                            (ProjectMembers innerJoin Users)
                                .selectAll().where { ProjectMembers.projectId eq projectId }
                                .map {
                                    ProjectMemberResponse(
                                        userId = it[Users.id].value.toString(),
                                        email = it[Users.email],
                                        name = it[Users.name],
                                        role = it[ProjectMembers.role].name,
                                        joinedAt = it[ProjectMembers.joinedAt].epochSeconds * 1000
                                    )
                                }
                        }

                        // Add owner if not in members
                        val project = transaction {
                            Projects.selectAll().where { Projects.id eq projectId }.firstOrNull()
                        }
                        if (project != null) {
                            val ownerId = project[Projects.ownerId]
                            val ownerInMembers = members.any { it.userId == ownerId.toString() }
                            if (!ownerInMembers) {
                                val owner = transaction {
                                    Users.selectAll().where { Users.id eq ownerId }.firstOrNull()
                                }
                                if (owner != null) {
                                    val ownerData = ProjectMemberResponse(
                                        userId = owner[Users.id].value.toString(),
                                        email = owner[Users.email],
                                        name = owner[Users.name],
                                        role = "OWNER",
                                        joinedAt = project[Projects.createdAt].epochSeconds * 1000
                                    )
                                    call.respond(listOf(ownerData) + members)
                                    return@get
                                }
                            }
                        }

                        call.respond(members)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to retrieve members: ${e.message}")
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
                    val userId = UUID.fromString(
                        call.getUserId() ?: return@post call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "Unauthorized")
                        )
                    )

                    if (!accessService.checkProjectRole(userId, projectId, ProjectRole.ADMIN)) {
                        return@post call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Insufficient permissions. ADMIN role or higher required")
                        )
                    }

                    try {
                        val request = call.receive<AddProjectMemberRequest>()
                        val userEmail = request.email
                        val roleStr = request.role

                        val role = try {
                            ProjectRole.valueOf(roleStr.uppercase())
                        } catch (_: Exception) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Invalid role. Valid roles: OWNER, ADMIN, MEMBER, VIEWER")
                            )
                        }

                        if (role == ProjectRole.OWNER) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Cannot assign OWNER role. Only project creator is owner.")
                            )
                        }

                        val result = transaction {
                            val targetUser =
                                Users.selectAll().where { Users.email eq userEmail }.firstOrNull()
                            if (targetUser == null) {
                                return@transaction null
                            }

                            val targetUserId = targetUser[Users.id].value

                            // Check if already a member
                            val existing = ProjectMembers.selectAll()
                                .where { (ProjectMembers.projectId eq projectId) and (ProjectMembers.userId eq targetUserId) }
                                .firstOrNull()

                            if (existing != null) {
                                // Update role
                                ProjectMembers.update({
                                    (ProjectMembers.projectId eq projectId) and (ProjectMembers.userId eq targetUserId)
                                }) {
                                    it[ProjectMembers.role] = role
                                }
                            } else {
                                // Add new member
                                ProjectMembers.insert {
                                    it[ProjectMembers.projectId] = projectId
                                    it[ProjectMembers.userId] = targetUserId
                                    it[ProjectMembers.role] = role
                                    it[ProjectMembers.joinedAt] = Clock.System.now()
                                }
                            }

                            ProjectMemberResponse(
                                userId = targetUserId.toString(),
                                email = userEmail,
                                name = targetUser[Users.name],
                                role = role.name,
                                joinedAt = System.currentTimeMillis()
                            )
                        }
                        
                        if (result == null) {
                            return@post call.respond(
                                HttpStatusCode.NotFound,
                                mapOf("error" to "User not found")
                            )
                        }
                        
                        auditService.log(
                            AuditAction.PROJECT_MEMBER_ADDED,
                            "project_member",
                            result.userId,
                            projectId,
                            userId,
                            mapOf("email" to userEmail, "role" to role.name),
                            call
                        )
                        
                        call.respond(result)
                    } catch (e: SerializationException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid request body format: ${e.message}")
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to add member: ${e.message}")
                        )
                    }
                }

                delete("{memberId}") {
                    val projectIdParam = call.parameters["projectId"]
                        ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing projectId")
                        )

                    val memberIdParam = call.parameters["memberId"]
                        ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Missing memberId")
                        )

                    if (!ValidationUtils.validateUUID(projectIdParam) || !ValidationUtils.validateUUID(
                            memberIdParam
                        )
                    ) {
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid UUID format")
                        )
                    }

                    val projectId = UUID.fromString(projectIdParam)
                    val memberId = UUID.fromString(memberIdParam)
                    val userId = UUID.fromString(
                        call.getUserId() ?: return@delete call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "Unauthorized")
                        )
                    )

                    if (!accessService.checkProjectRole(userId, projectId, ProjectRole.ADMIN)) {
                        return@delete call.respond(
                            HttpStatusCode.Forbidden,
                            mapOf("error" to "Insufficient permissions. ADMIN role or higher required")
                        )
                    }

                    // Cannot remove owner
                    val project = transaction {
                        Projects.selectAll().where { Projects.id eq projectId }.firstOrNull()
                    }
                    if (project != null && project[Projects.ownerId] == memberId) {
                        return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Cannot remove project owner")
                        )
                    }

                    try {
                        val deleted = transaction {
                            val member = ProjectMembers.selectAll()
                                .where { (ProjectMembers.projectId eq projectId) and (ProjectMembers.userId eq memberId) }
                                .firstOrNull()

                            if (member != null) {
                                ProjectMembers.deleteWhere {
                                    (ProjectMembers.projectId eq projectId) and (ProjectMembers.userId eq memberId)
                                }
                                true
                            } else {
                                false
                            }
                        }

                        if (!deleted) {
                            return@delete call.respond(
                                HttpStatusCode.NotFound,
                                mapOf("error" to "Member not found")
                            )
                        }

                        auditService.log(
                            AuditAction.PROJECT_MEMBER_REMOVED,
                            "project_member",
                            memberId.toString(),
                            projectId,
                            userId,
                            emptyMap(),
                            call
                        )

                        call.respond(
                            HttpStatusCode.OK,
                            mapOf("message" to "Member removed successfully")
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to remove member: ${e.message}")
                        )
                    }
                }
            }
        }
    }
}

