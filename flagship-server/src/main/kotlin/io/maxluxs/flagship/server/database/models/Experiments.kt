package io.maxluxs.flagship.server.database.models

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object Experiments : UUIDTable("experiments") {
    val projectId = uuid("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val key = varchar("key", 255)
    val config = text("config") // JSON serialized RestExperiment
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val createdBy = uuid("created_by").references(Users.id).nullable()

    init {
        uniqueIndex(projectId, key)
        index(isUnique = false, projectId, key)
        index(isUnique = false, projectId, isActive)
        index(isUnique = false, createdAt)
        index(isUnique = false, updatedAt)
    }
}

