package io.maxluxs.flagship.server.auth

import io.maxluxs.flagship.server.database.models.ProjectMembers
import io.maxluxs.flagship.server.database.models.ProjectRole
import io.maxluxs.flagship.server.database.models.Projects
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class ProjectAccessService {

    fun checkProjectAccess(userId: UUID, projectId: UUID): Boolean {
        return transaction {
            val member = ProjectMembers.selectAll()
                .where { (ProjectMembers.projectId eq projectId) and (ProjectMembers.userId eq userId) }
                .firstOrNull()

            if (member != null) {
                return@transaction true
            }

            val project = Projects.selectAll().where { Projects.id eq projectId }.firstOrNull()
            if (project != null && project[Projects.ownerId] == userId) {
                return@transaction true
            }

            false
        }
    }

    fun checkProjectRole(userId: UUID, projectId: UUID, minRole: ProjectRole): Boolean {
        return transaction {
            val member = ProjectMembers.selectAll()
                .where { (ProjectMembers.projectId eq projectId) and (ProjectMembers.userId eq userId) }
                .firstOrNull()

            if (member != null) {
                val role = member[ProjectMembers.role]
                return@transaction hasMinimumRole(role, minRole)
            }

            val project = Projects.selectAll().where { Projects.id eq projectId }.firstOrNull()
            if (project != null && project[Projects.ownerId] == userId) {
                return@transaction true
            }

            false
        }
    }

    fun getUserProjectRole(userId: UUID, projectId: UUID): ProjectRole? {
        return transaction {
            val member = ProjectMembers.selectAll()
                .where { (ProjectMembers.projectId eq projectId) and (ProjectMembers.userId eq userId) }
                .firstOrNull()

            if (member != null) {
                return@transaction member[ProjectMembers.role]
            }

            val project = Projects.selectAll().where { Projects.id eq projectId }.firstOrNull()
            if (project != null && project[Projects.ownerId] == userId) {
                return@transaction ProjectRole.OWNER
            }

            null
        }
    }

    private fun hasMinimumRole(userRole: ProjectRole, minRole: ProjectRole): Boolean {
        val roleHierarchy = mapOf(
            ProjectRole.OWNER to 4,
            ProjectRole.ADMIN to 3,
            ProjectRole.MEMBER to 2,
            ProjectRole.VIEWER to 1
        )

        val userLevel = roleHierarchy[userRole] ?: 0
        val minLevel = roleHierarchy[minRole] ?: 0

        return userLevel >= minLevel
    }
}
