package io.maxluxs.flagship.server.database.models

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

enum class ProjectRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER
}

@OptIn(ExperimentalTime::class)
object ProjectMembers : UUIDTable("project_members") {
    val projectId = uuid("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val userId = uuid("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val role = enumeration<ProjectRole>("role").default(ProjectRole.MEMBER)
    val joinedAt = timestamp("joined_at")

    init {
        uniqueIndex(projectId, userId)
        index(isUnique = false, projectId, userId)
        index(isUnique = false, projectId)
    }
}

