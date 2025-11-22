package io.maxluxs.flagship.server.database.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import kotlinx.datetime.Clock

object Experiments : UUIDTable("experiments") {
    val projectId = uuid("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val key = varchar("key", 255)
    val config = text("config") // JSON serialized RestExperiment
    val description = text("description").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val createdBy = uuid("created_by").references(Users.id).nullable()
    
    init {
        uniqueIndex(projectId, key)
    }
}

