package io.maxluxs.flagship.server.database.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import kotlinx.datetime.Clock

enum class ProjectRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER
}

object ProjectMembers : UUIDTable("project_members") {
    val projectId = uuid("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val userId = uuid("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val role = enumeration<ProjectRole>("role").default(ProjectRole.MEMBER)
    val joinedAt = timestamp("joined_at")
    
    init {
        uniqueIndex(projectId, userId)
    }
}

