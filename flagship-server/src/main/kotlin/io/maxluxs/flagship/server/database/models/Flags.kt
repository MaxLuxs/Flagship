package io.maxluxs.flagship.server.database.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import kotlinx.datetime.Clock

object Flags : UUIDTable("flags") {
    val projectId = uuid("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val key = varchar("key", 255)
    val type = varchar("type", 50) // bool, string, number
    val value = text("value") // JSON serialized value
    val description = text("description").nullable()
    val isEnabled = bool("is_enabled").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val createdBy = uuid("created_by").references(Users.id).nullable()
    
    init {
        uniqueIndex(projectId, key)
    }
}

