package io.maxluxs.flagship.server.audit

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import io.maxluxs.flagship.server.database.models.AuditLogs
import io.maxluxs.flagship.server.database.models.AuditAction
import kotlinx.serialization.json.Json

class AuditService {
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun log(
        action: AuditAction,
        entityType: String,
        entityId: String? = null,
        projectId: UUID? = null,
        userId: UUID? = null,
        changes: Map<String, Any?>? = null,
        call: ApplicationCall? = null
    ) {
        transaction {
            AuditLogs.insert {
                it[AuditLogs.action] = action
                it[AuditLogs.entityType] = entityType
                it[AuditLogs.entityId] = entityId
                it[AuditLogs.projectId] = projectId
                it[AuditLogs.userId] = userId
                it[AuditLogs.changes] = changes?.let { json.encodeToString(it) }
                it[AuditLogs.ipAddress] = call?.request?.headers?.get("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
                    ?: call?.request?.headers?.get("X-Real-IP")
                    ?: "unknown"
                it[AuditLogs.userAgent] = call?.request?.headers?.get("User-Agent")
            }
        }
    }
    
    suspend fun getLogs(
        projectId: UUID? = null,
        userId: UUID? = null,
        limit: Int = 100
    ): List<AuditLogEntry> = transaction {
        var query = AuditLogs.selectAll()
        
        if (projectId != null) {
            query = query.andWhere { AuditLogs.projectId eq projectId }
        }
        
        if (userId != null) {
            query = query.andWhere { AuditLogs.userId eq userId }
        }
        
        query
            .orderBy(AuditLogs.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { row ->
                AuditLogEntry(
                    id = row[AuditLogs.id].value.toString(),
                    action = row[AuditLogs.action].name,
                    entityType = row[AuditLogs.entityType],
                    entityId = row[AuditLogs.entityId],
                    projectId = row[AuditLogs.projectId]?.toString(),
                    userId = row[AuditLogs.userId]?.toString(),
                    changes = row[AuditLogs.changes]?.let { 
                        try {
                            val jsonObj = json.parseToJsonElement(it)
                            if (jsonObj is kotlinx.serialization.json.JsonObject) {
                                jsonObj.entries.associate { (k, v) -> k to v.toString() }
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    },
                    ipAddress = row[AuditLogs.ipAddress],
                    userAgent = row[AuditLogs.userAgent],
                    createdAt = row[AuditLogs.createdAt].epochSeconds
                )
            }
    }
}

data class AuditLogEntry(
    val id: String,
    val action: String,
    val entityType: String,
    val entityId: String?,
    val projectId: String?,
    val userId: String?,
    val changes: Map<String, Any?>?,
    val ipAddress: String?,
    val userAgent: String?,
    val createdAt: Long
)

