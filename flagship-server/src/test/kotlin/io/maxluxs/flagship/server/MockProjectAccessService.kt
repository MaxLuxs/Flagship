package io.maxluxs.flagship.server

import io.maxluxs.flagship.server.auth.ProjectAccessService
import io.maxluxs.flagship.server.database.models.ProjectRole
import java.util.*

class MockProjectAccessService(
    private val allowedProjects: Map<UUID, Set<UUID>> = emptyMap(),
    private val userRoles: Map<Pair<UUID, UUID>, ProjectRole> = emptyMap()
) {
    
    private val accessMap = allowedProjects.toMutableMap()
    private val rolesMap = userRoles.toMutableMap()
    
    fun checkProjectAccess(userId: UUID, projectId: UUID): Boolean {
        return accessMap[projectId]?.contains(userId) ?: false
    }
    
    fun checkProjectRole(userId: UUID, projectId: UUID, minRole: ProjectRole): Boolean {
        val userRole = rolesMap[Pair(userId, projectId)] ?: return false
        return hasMinimumRole(userRole, minRole)
    }
    
    fun getUserProjectRole(userId: UUID, projectId: UUID): ProjectRole? {
        return rolesMap[Pair(userId, projectId)]
    }
    
    fun allowAccess(userId: UUID, projectId: UUID, role: ProjectRole = ProjectRole.OWNER) {
        accessMap.getOrPut(projectId) { mutableSetOf() }.add(userId)
        rolesMap[Pair(userId, projectId)] = role
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
