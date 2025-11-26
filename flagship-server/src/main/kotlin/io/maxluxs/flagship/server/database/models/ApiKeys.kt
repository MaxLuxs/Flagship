package io.maxluxs.flagship.server.database.models

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

enum class ApiKeyType {
    READ_ONLY,
    ADMIN
}

@OptIn(ExperimentalTime::class)
object ApiKeys : UUIDTable("api_keys") {
    val projectId = uuid("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val keyHash = varchar("key_hash", 255).uniqueIndex()
    val name = varchar("name", 255)
    val type = enumeration<ApiKeyType>("type").default(ApiKeyType.READ_ONLY)
    val lastUsedAt = timestamp("last_used_at").nullable()
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at").nullable()
    val isActive = bool("is_active").default(true)

    init {
        index(isUnique = false, projectId)
        index(isUnique = false, projectId, isActive)
        index(isUnique = false, createdAt)
    }
}

