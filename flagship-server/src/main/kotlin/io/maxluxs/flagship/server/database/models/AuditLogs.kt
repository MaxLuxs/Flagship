package io.maxluxs.flagship.server.database.models

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

enum class AuditAction {
    FLAG_CREATED,
    FLAG_UPDATED,
    FLAG_DELETED,
    FLAG_TOGGLED,
    EXPERIMENT_CREATED,
    EXPERIMENT_UPDATED,
    EXPERIMENT_DELETED,
    PROJECT_CREATED,
    PROJECT_UPDATED,
    PROJECT_DELETED,
    PROJECT_MEMBER_ADDED,
    PROJECT_MEMBER_REMOVED,
    USER_CREATED,
    USER_UPDATED,
    API_KEY_CREATED,
    API_KEY_DELETED
}

@OptIn(ExperimentalTime::class)
object AuditLogs : UUIDTable("audit_logs") {
    val projectId =
        uuid("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE).nullable()
    val userId =
        uuid("user_id").references(Users.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val action = enumeration<AuditAction>("action")
    val entityType = varchar("entity_type", 50) // flag, experiment, project, user, api_key
    val entityId = varchar("entity_id", 255).nullable()
    val changes = text("changes").nullable() // JSON with old/new values
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val createdAt = timestamp("created_at")

    init {
        index(isUnique = false, projectId, createdAt)
        index(isUnique = false, userId, createdAt)
        index(isUnique = false, action, createdAt)
        index(isUnique = false, createdAt)
    }
}

