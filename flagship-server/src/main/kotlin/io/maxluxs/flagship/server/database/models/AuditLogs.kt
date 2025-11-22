package io.maxluxs.flagship.server.database.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import kotlinx.datetime.Clock

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
    USER_CREATED,
    USER_UPDATED,
    API_KEY_CREATED,
    API_KEY_DELETED
}

object AuditLogs : UUIDTable("audit_logs") {
    val projectId = uuid("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE).nullable()
    val userId = uuid("user_id").references(Users.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val action = enumeration<AuditAction>("action")
    val entityType = varchar("entity_type", 50) // flag, experiment, project, user, api_key
    val entityId = varchar("entity_id", 255).nullable()
    val changes = text("changes").nullable() // JSON with old/new values
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val createdAt = timestamp("created_at")
}

