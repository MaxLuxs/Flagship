package io.maxluxs.flagship.server.database.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import kotlinx.datetime.Clock

enum class ApiKeyType {
    READ_ONLY,
    ADMIN
}

object ApiKeys : UUIDTable("api_keys") {
    val projectId = uuid("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val keyHash = varchar("key_hash", 255).uniqueIndex()
    val name = varchar("name", 255)
    val type = enumeration<ApiKeyType>("type").default(ApiKeyType.READ_ONLY)
    val lastUsedAt = timestamp("last_used_at").nullable()
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at").nullable()
    val isActive = bool("is_active").default(true)
}

