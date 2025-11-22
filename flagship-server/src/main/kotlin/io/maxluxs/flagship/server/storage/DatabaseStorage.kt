package io.maxluxs.flagship.server.storage

import io.maxluxs.flagship.provider.rest.RestExperiment
import io.maxluxs.flagship.provider.rest.RestFlagValue
import io.maxluxs.flagship.provider.rest.RestResponse
import io.maxluxs.flagship.server.Storage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class DatabaseStorage : Storage {
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun getAllFlags(projectId: UUID): Map<String, RestFlagValue> = transaction {
        io.maxluxs.flagship.server.database.models.Flags
            .select { 
                (io.maxluxs.flagship.server.database.models.Flags.projectId eq projectId) and
                (io.maxluxs.flagship.server.database.models.Flags.isEnabled eq true)
            }
            .associate { row ->
                val key = row[io.maxluxs.flagship.server.database.models.Flags.key]
                val type = row[io.maxluxs.flagship.server.database.models.Flags.type]
                val value = row[io.maxluxs.flagship.server.database.models.Flags.value]
                
                // Parse value based on type
                val jsonValue: JsonElement = when (type) {
                    "bool" -> JsonPrimitive(value.toBoolean())
                    "number" -> JsonPrimitive(value.toDoubleOrNull() ?: 0.0)
                    "string" -> JsonPrimitive(value)
                    else -> json.parseToJsonElement(value)
                }
                key to RestFlagValue(type = type, value = jsonValue)
            }
    }
    
    override suspend fun getFlag(projectId: UUID, key: String): RestFlagValue? = transaction {
        io.maxluxs.flagship.server.database.models.Flags
            .select { 
                (io.maxluxs.flagship.server.database.models.Flags.projectId eq projectId) and
                (io.maxluxs.flagship.server.database.models.Flags.key eq key)
            }
            .firstOrNull()?.let { row ->
                val type = row[io.maxluxs.flagship.server.database.models.Flags.type]
                val value = row[io.maxluxs.flagship.server.database.models.Flags.value]
                val jsonValue: JsonElement = when (type) {
                    "bool" -> JsonPrimitive(value.toBoolean())
                    "number" -> JsonPrimitive(value.toDoubleOrNull() ?: 0.0)
                    "string" -> JsonPrimitive(value)
                    else -> json.parseToJsonElement(value)
                }
                RestFlagValue(type = type, value = jsonValue)
            }
    }
    
    override suspend fun createFlag(
        projectId: UUID,
        key: String,
        flag: RestFlagValue,
        userId: UUID?
    ): RestFlagValue = transaction {
        // Serialize JsonElement to string based on type
        val valueStr: String = when {
            flag.value is JsonPrimitive -> (flag.value as JsonPrimitive).content
            else -> json.encodeToString(flag.value)
        }
        
        io.maxluxs.flagship.server.database.models.Flags.insert {
            it[io.maxluxs.flagship.server.database.models.Flags.projectId] = projectId
            it[io.maxluxs.flagship.server.database.models.Flags.key] = key
            it[io.maxluxs.flagship.server.database.models.Flags.type] = flag.type
            it[io.maxluxs.flagship.server.database.models.Flags.value] = valueStr
            it[io.maxluxs.flagship.server.database.models.Flags.isEnabled] = true
            it[io.maxluxs.flagship.server.database.models.Flags.createdBy] = userId
        }
        
        flag
    }
    
    override suspend fun updateFlag(
        projectId: UUID,
        key: String,
        flag: RestFlagValue
    ): RestFlagValue? = transaction {
        val valueStr: String = when {
            flag.value is JsonPrimitive -> (flag.value as JsonPrimitive).content
            else -> json.encodeToString(flag.value)
        }
        
        val updated = io.maxluxs.flagship.server.database.models.Flags
            .update({ 
                (io.maxluxs.flagship.server.database.models.Flags.projectId eq projectId) and
                (io.maxluxs.flagship.server.database.models.Flags.key eq key)
            }) {
                it[io.maxluxs.flagship.server.database.models.Flags.type] = flag.type
                it[io.maxluxs.flagship.server.database.models.Flags.value] = valueStr
                it[io.maxluxs.flagship.server.database.models.Flags.updatedAt] = Clock.System.now()
            }
        
        if (updated > 0) flag else null
    }
    
    override suspend fun deleteFlag(projectId: UUID, key: String): Boolean = transaction {
        val deleted = io.maxluxs.flagship.server.database.models.Flags
            .deleteWhere { 
                (io.maxluxs.flagship.server.database.models.Flags.projectId eq projectId) and
                (io.maxluxs.flagship.server.database.models.Flags.key eq key)
            }
        deleted > 0
    }
    
    override suspend fun getAllExperiments(projectId: UUID): Map<String, RestExperiment> = transaction {
        io.maxluxs.flagship.server.database.models.Experiments
            .select { io.maxluxs.flagship.server.database.models.Experiments.projectId eq projectId }
            .associate { row ->
                val key = row[io.maxluxs.flagship.server.database.models.Experiments.key]
                val config = row[io.maxluxs.flagship.server.database.models.Experiments.config]
                key to json.decodeFromString<RestExperiment>(config)
            }
    }
    
    override suspend fun getExperiment(projectId: UUID, key: String): RestExperiment? = transaction {
        io.maxluxs.flagship.server.database.models.Experiments
            .select { 
                (io.maxluxs.flagship.server.database.models.Experiments.projectId eq projectId) and
                (io.maxluxs.flagship.server.database.models.Experiments.key eq key)
            }
            .firstOrNull()?.let { row ->
                val config = row[io.maxluxs.flagship.server.database.models.Experiments.config]
                json.decodeFromString<RestExperiment>(config)
            }
    }
    
    override suspend fun createExperiment(
        projectId: UUID,
        key: String,
        experiment: RestExperiment,
        userId: UUID?
    ): RestExperiment = transaction {
        io.maxluxs.flagship.server.database.models.Experiments.insert {
            it[io.maxluxs.flagship.server.database.models.Experiments.projectId] = projectId
            it[io.maxluxs.flagship.server.database.models.Experiments.key] = key
            it[io.maxluxs.flagship.server.database.models.Experiments.config] = json.encodeToString(experiment)
            it[io.maxluxs.flagship.server.database.models.Experiments.isActive] = true
            it[io.maxluxs.flagship.server.database.models.Experiments.createdBy] = userId
        }
        experiment
    }
    
    override suspend fun updateExperiment(
        projectId: UUID,
        key: String,
        experiment: RestExperiment
    ): RestExperiment? = transaction {
        val updated = io.maxluxs.flagship.server.database.models.Experiments
            .update({ 
                (io.maxluxs.flagship.server.database.models.Experiments.projectId eq projectId) and
                (io.maxluxs.flagship.server.database.models.Experiments.key eq key)
            }) {
                it[io.maxluxs.flagship.server.database.models.Experiments.config] = json.encodeToString(experiment)
                it[io.maxluxs.flagship.server.database.models.Experiments.updatedAt] = Clock.System.now()
            }
        
        if (updated > 0) experiment else null
    }
    
    override suspend fun deleteExperiment(projectId: UUID, key: String): Boolean = transaction {
        val deleted = io.maxluxs.flagship.server.database.models.Experiments
            .deleteWhere { 
                (io.maxluxs.flagship.server.database.models.Experiments.projectId eq projectId) and
                (io.maxluxs.flagship.server.database.models.Experiments.key eq key)
            }
        deleted > 0
    }
    
    override suspend fun getConfig(projectId: UUID, revision: String?): RestResponse {
        val flags = getAllFlags(projectId)
        val experiments = getAllExperiments(projectId)
        val currentRevision = generateRevision()
        
        return RestResponse(
            revision = currentRevision,
            fetchedAt = System.currentTimeMillis(),
            ttlMs = 900_000L,
            flags = flags,
            experiments = experiments
        )
    }
    
    override fun generateRevision(): String = UUID.randomUUID().toString()
}

