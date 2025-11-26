package io.maxluxs.flagship.server.storage

import io.maxluxs.flagship.server.ExperimentMetadata
import io.maxluxs.flagship.server.FlagMetadata
import io.maxluxs.flagship.server.Storage
import io.maxluxs.flagship.server.database.models.Experiments
import io.maxluxs.flagship.server.database.models.Flags
import io.maxluxs.flagship.server.database.models.Projects
import io.maxluxs.flagship.shared.api.FlagResponse
import io.maxluxs.flagship.shared.api.RestExperiment
import io.maxluxs.flagship.shared.api.RestFlagValue
import io.maxluxs.flagship.shared.api.RestResponse
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DatabaseStorage : Storage {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val logger = LoggerFactory.getLogger(DatabaseStorage::class.java)

    private suspend fun <T> withRetry(
        maxRetries: Int = 3,
        delayMs: Long = 100,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (e.isDeadlockException() && attempt < maxRetries - 1) {
                    logger.warn(
                        "Deadlock detected, retrying (attempt ${attempt + 1}/$maxRetries)",
                        e
                    )
                    delay(delayMs * (attempt + 1))
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: Exception("Unknown error")
    }

    private fun Exception.isDeadlockException(): Boolean {
        return this is PSQLException &&
                (message?.contains("deadlock", ignoreCase = true) == true ||
                        message?.contains("could not serialize", ignoreCase = true) == true)
    }

    private fun parseFlagValue(type: String, value: String): JsonElement {
        return try {
            when (type.lowercase()) {
                "bool" -> {
                    val boolValue = when (value.lowercase()) {
                        "true", "1", "yes" -> true
                        "false", "0", "no" -> false
                        else -> value.toBooleanStrictOrNull() ?: false
                    }
                    JsonPrimitive(boolValue)
                }

                "number", "int", "double" -> {
                    val numValue = value.toDoubleOrNull() ?: value.toLongOrNull()?.toDouble() ?: 0.0
                    JsonPrimitive(numValue)
                }

                "string" -> JsonPrimitive(value)
                "json" -> {
                    try {
                        json.parseToJsonElement(value)
                    } catch (e: Exception) {
                        logger.warn(
                            "Failed to parse JSON value for type 'json', using as string: $value",
                            e
                        )
                        JsonPrimitive(value)
                    }
                }

                else -> {
                    logger.warn("Unknown flag type '$type', attempting to parse as JSON")
                    try {
                        json.parseToJsonElement(value)
                    } catch (e: Exception) {
                        JsonPrimitive(value)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error parsing flag value (type=$type, value=$value)", e)
            JsonPrimitive(value)
        }
    }

    private fun serializeFlagValue(flag: RestFlagValue): String {
        return when {
            flag.value is JsonPrimitive -> {
                val primitive = flag.value as JsonPrimitive
                when {
                    primitive.isString -> primitive.content
                    else -> primitive.content
                }
            }

            else -> {
                try {
                    json.encodeToString(JsonElement.serializer(), flag.value)
                } catch (e: Exception) {
                    logger.error("Failed to serialize flag value", e)
                    flag.value.toString()
                }
            }
        }
    }

    private suspend fun checkProjectExists(projectId: UUID): Boolean {
        return suspendTransaction {
            Projects
                .selectAll().where { Projects.id eq projectId }
                .count() > 0
        }
    }

    override suspend fun getAllFlags(projectId: UUID): Map<String, RestFlagValue> = withRetry {
        suspendTransaction {
            try {
                if (!checkProjectExists(projectId)) {
                    logger.warn("Project $projectId does not exist")
                    return@suspendTransaction emptyMap()
                }

                Flags
                    .selectAll().where {
                        (Flags.projectId eq projectId) and
                                (Flags.isEnabled eq true)
                    }
                    .associate { row ->
                        try {
                            val key = row[Flags.key]
                            val type = row[Flags.type]
                            val value =
                                row[Flags.value] ?: ""

                            val jsonValue = parseFlagValue(type, value)
                            key to RestFlagValue(type = type, value = jsonValue)
                        } catch (e: Exception) {
                            logger.error("Error parsing flag row", e)
                            throw e
                        }
                    }
            } catch (e: Exception) {
                logger.error("Error getting all flags for project $projectId", e)
                throw e
            }
        }
    }

    override suspend fun getFlag(projectId: UUID, key: String): RestFlagValue? = withRetry {
        suspendTransaction {
            try {
                Flags
                    .selectAll().where {
                        (Flags.projectId eq projectId) and
                                (Flags.key eq key)
                    }
                    .firstOrNull()?.let { row ->
                        try {
                            val type = row[Flags.type]
                            val value = row[Flags.value]
                            val jsonValue = parseFlagValue(type, value)
                            RestFlagValue(type = type, value = jsonValue)
                        } catch (e: Exception) {
                            logger.error("Error parsing flag (projectId=$projectId, key=$key)", e)
                            null
                        }
                    }
            } catch (e: Exception) {
                logger.error("Error getting flag (projectId=$projectId, key=$key)", e)
                throw e
            }
        }
    }

    override suspend fun createFlag(
        projectId: UUID,
        key: String,
        flag: RestFlagValue,
        userId: UUID?
    ): RestFlagValue = withRetry {
        suspendTransaction {
            try {
                if (!checkProjectExists(projectId)) {
                    throw IllegalArgumentException("Project $projectId does not exist")
                }

                // Validate flag type
                val validTypes = setOf("bool", "int", "double", "number", "string", "json")
                if (!validTypes.contains(flag.type.lowercase())) {
                    throw IllegalArgumentException("Invalid flag type: ${flag.type}. Valid types: $validTypes")
                }

                val valueStr = serializeFlagValue(flag)

                Flags.insert {
                    it[Flags.projectId] = projectId
                    it[Flags.key] = key
                    it[Flags.type] = flag.type.lowercase()
                    it[Flags.value] = valueStr
                    it[Flags.isEnabled] = true
                    it[Flags.createdBy] = userId
                    it[Flags.createdAt] = Clock.System.now()
                    it[Flags.updatedAt] = Clock.System.now()
                }

                flag
            } catch (e: Exception) {
                logger.error("Error creating flag (projectId=$projectId, key=$key)", e)
                throw e
            }
        }
    }

    override suspend fun updateFlag(
        projectId: UUID,
        key: String,
        flag: RestFlagValue
    ): RestFlagValue? = withRetry {
        suspendTransaction {
            try {
                // Validate flag type
                val validTypes = setOf("bool", "int", "double", "number", "string", "json")
                if (!validTypes.contains(flag.type.lowercase())) {
                    throw IllegalArgumentException("Invalid flag type: ${flag.type}. Valid types: $validTypes")
                }

                val valueStr = serializeFlagValue(flag)

                val updated = Flags
                    .update({
                        (Flags.projectId eq projectId) and
                                (Flags.key eq key)
                    }) {
                        it[Flags.type] = flag.type.lowercase()
                        it[Flags.value] = valueStr
                        it[Flags.updatedAt] = Clock.System.now()
                    }

                if (updated > 0) flag else null
            } catch (e: Exception) {
                logger.error("Error updating flag (projectId=$projectId, key=$key)", e)
                throw e
            }
        }
    }

    override suspend fun deleteFlag(projectId: UUID, key: String): Boolean = withRetry {
        suspendTransaction {
            try {
                val deleted = Flags
                    .deleteWhere {
                        (Flags.projectId eq projectId) and
                                (Flags.key eq key)
                    }
                deleted > 0
            } catch (e: Exception) {
                logger.error("Error deleting flag (projectId=$projectId, key=$key)", e)
                throw e
            }
        }
    }

    override suspend fun getAllExperiments(projectId: UUID): Map<String, RestExperiment> =
        withRetry {
            suspendTransaction {
                try {
                    if (!checkProjectExists(projectId)) {
                        logger.warn("Project $projectId does not exist")
                        return@suspendTransaction emptyMap()
                    }

                    Experiments
                        .selectAll().where { Experiments.projectId eq projectId }
                        .mapNotNull { row ->
                            try {
                                val key = row[Experiments.key]
                                val config = row[Experiments.config]
                                key to json.decodeFromString<RestExperiment>(config)
                            } catch (e: Exception) {
                                logger.error(
                                    "Error parsing experiment row (projectId=$projectId)",
                                    e
                                )
                                null
                            }
                        }
                        .toMap()
                } catch (e: Exception) {
                    logger.error("Error getting all experiments for project $projectId", e)
                    throw e
                }
            }
        }

    override suspend fun getExperiment(projectId: UUID, key: String): RestExperiment? = withRetry {
        suspendTransaction {
            try {
                Experiments
                    .selectAll().where {
                        (Experiments.projectId eq projectId) and
                                (Experiments.key eq key)
                    }
                    .firstOrNull()?.let { row ->
                        try {
                            val config = row[Experiments.config] ?: return@let null
                            json.decodeFromString<RestExperiment>(config)
                        } catch (e: Exception) {
                            logger.error(
                                "Error parsing experiment (projectId=$projectId, key=$key)",
                                e
                            )
                            null
                        }
                    }
            } catch (e: Exception) {
                logger.error("Error getting experiment (projectId=$projectId, key=$key)", e)
                throw e
            }
        }
    }

    override suspend fun createExperiment(
        projectId: UUID,
        key: String,
        experiment: RestExperiment,
        userId: UUID?
    ): RestExperiment = withRetry {
        suspendTransaction {
            try {
                if (!checkProjectExists(projectId)) {
                    throw IllegalArgumentException("Project $projectId does not exist")
                }

                // Validate experiment
                validateExperiment(experiment)

                val configStr = try {
                    json.encodeToString(RestExperiment.serializer(), experiment)
                } catch (e: Exception) {
                    logger.error("Error serializing experiment", e)
                    throw IllegalArgumentException("Invalid experiment configuration", e)
                }

                Experiments.insert {
                    it[Experiments.projectId] = projectId
                    it[Experiments.key] = key
                    it[Experiments.config] = configStr
                    it[Experiments.isActive] = true
                    it[Experiments.createdBy] = userId
                    it[Experiments.createdAt] = Clock.System.now()
                    it[Experiments.updatedAt] = Clock.System.now()
                }
                experiment
            } catch (e: Exception) {
                logger.error("Error creating experiment (projectId=$projectId, key=$key)", e)
                throw e
            }
        }
    }

    override suspend fun updateExperiment(
        projectId: UUID,
        key: String,
        experiment: RestExperiment
    ): RestExperiment? = withRetry {
        suspendTransaction {
            try {
                // Validate experiment
                validateExperiment(experiment)

                val configStr = try {
                    json.encodeToString(RestExperiment.serializer(), experiment)
                } catch (e: Exception) {
                    logger.error("Error serializing experiment", e)
                    throw IllegalArgumentException("Invalid experiment configuration", e)
                }

                val updated = Experiments
                    .update({
                        (Experiments.projectId eq projectId) and
                                (Experiments.key eq key)
                    }) {
                        it[Experiments.config] = configStr
                        it[Experiments.updatedAt] = Clock.System.now()
                    }

                if (updated > 0) experiment else null
            } catch (e: Exception) {
                logger.error("Error updating experiment (projectId=$projectId, key=$key)", e)
                throw e
            }
        }
    }

    override suspend fun deleteExperiment(projectId: UUID, key: String): Boolean = withRetry {
        suspendTransaction {
            try {
                val deleted = Experiments
                    .deleteWhere {
                        (Experiments.projectId eq projectId) and
                                (Experiments.key eq key)
                    }
                deleted > 0
            } catch (e: Exception) {
                logger.error("Error deleting experiment (projectId=$projectId, key=$key)", e)
                throw e
            }
        }
    }

    override suspend fun getConfig(projectId: UUID, revision: String?): RestResponse {
        return try {
            val flags = getAllFlags(projectId)
            val experiments = getAllExperiments(projectId)
            val currentRevision = generateRevision()

            RestResponse(
                revision = currentRevision,
                fetchedAt = System.currentTimeMillis(),
                ttlMs = 900_000L,
                flags = flags,
                experiments = experiments
            )
        } catch (e: Exception) {
            logger.error("Error getting config for project $projectId", e)
            throw e
        }
    }

    override fun generateRevision(): String = UUID.randomUUID().toString()

    override suspend fun flagExists(projectId: UUID, key: String): Boolean = withRetry {
        suspendTransaction {
            Flags
                .selectAll().where {
                    (Flags.projectId eq projectId) and
                            (Flags.key eq key)
                }
                .count() > 0
        }
    }

    override suspend fun experimentExists(projectId: UUID, key: String): Boolean = withRetry {
        suspendTransaction {
            Experiments
                .selectAll().where {
                    (Experiments.projectId eq projectId) and
                            (Experiments.key eq key)
                }
                .count() > 0
        }
    }

    override suspend fun getFlagMetadata(projectId: UUID, key: String): FlagMetadata? = withRetry {
        suspendTransaction {
            Flags
                .selectAll().where {
                    (Flags.projectId eq projectId) and
                            (Flags.key eq key)
                }
                .firstOrNull()?.let { row ->
                    FlagMetadata(
                        createdAt = row[Flags.createdAt].epochSeconds * 1000,
                        updatedAt = row[Flags.updatedAt].epochSeconds * 1000,
                        createdBy = row[Flags.createdBy]
                    )
                }
        }
    }

    override suspend fun getExperimentMetadata(projectId: UUID, key: String): ExperimentMetadata? =
        withRetry {
            suspendTransaction {
                Experiments
                    .selectAll().where {
                        (Experiments.projectId eq projectId) and
                                (Experiments.key eq key)
                    }
                    .firstOrNull()?.let { row ->
                        ExperimentMetadata(
                            createdAt = row[Experiments.createdAt].epochSeconds * 1000,
                            updatedAt = row[Experiments.updatedAt].epochSeconds * 1000,
                            createdBy = row[Experiments.createdBy],
                            isActive = row[Experiments.isActive]
                        )
                    }
            }
        }

    private fun validateExperiment(experiment: RestExperiment) {
        // Validate variants
        if (experiment.variants.isEmpty()) {
            throw IllegalArgumentException("Experiment must have at least one variant")
        }

        // Check unique variant names
        val variantNames = experiment.variants.map { it.name }
        if (variantNames.size != variantNames.toSet().size) {
            throw IllegalArgumentException("Experiment variants must have unique names")
        }

        // Validate weights sum to 1.0 (with small tolerance)
        val totalWeight = experiment.variants.sumOf { it.weight }
        if (totalWeight < 0.99 || totalWeight > 1.01) {
            throw IllegalArgumentException("Experiment variant weights must sum to 1.0 (current: $totalWeight)")
        }

        // Validate weights are non-negative
        if (experiment.variants.any { it.weight < 0 }) {
            throw IllegalArgumentException("Experiment variant weights must be non-negative")
        }
    }

    override suspend fun getAllFlagsDetailed(projectId: UUID): List<FlagResponse> =
        withRetry {
            suspendTransaction {
                try {
                    if (!checkProjectExists(projectId)) {
                        logger.warn("Project $projectId does not exist")
                        return@suspendTransaction emptyList()
                    }

                    Flags
                        .selectAll().where { Flags.projectId eq projectId }
                        .mapNotNull { row ->
                            try {
                                val key = row[Flags.key]
                                val type = row[Flags.type]
                                val value = row[Flags.value]
                                val description = row[Flags.description]
                                val isEnabled = row[Flags.isEnabled]
                                val createdAt = row[Flags.createdAt].epochSeconds * 1000
                                val updatedAt = row[Flags.updatedAt].epochSeconds * 1000

                                FlagResponse(
                                    key = key,
                                    type = type,
                                    value = value,
                                    description = description,
                                    isEnabled = isEnabled,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt
                                )
                            } catch (e: Exception) {
                                logger.error("Error parsing flag row for detailed response", e)
                                null
                            }
                        }
                } catch (e: Exception) {
                    logger.error("Error getting all flags detailed for project $projectId", e)
                    throw e
                }
            }
        }

    override suspend fun toggleFlag(
        projectId: UUID,
        key: String
    ): FlagResponse? = withRetry {
        suspendTransaction {
            try {
                val flagRow = Flags
                    .selectAll().where {
                        (Flags.projectId eq projectId) and
                                (Flags.key eq key)
                    }
                    .firstOrNull()

                if (flagRow == null) {
                    return@suspendTransaction null
                }

                val currentIsEnabled = flagRow[Flags.isEnabled]
                val newIsEnabled = !currentIsEnabled

                val updated = Flags
                    .update({
                        (Flags.projectId eq projectId) and
                                (Flags.key eq key)
                    }) {
                        it[Flags.isEnabled] = newIsEnabled
                        it[Flags.updatedAt] = Clock.System.now()
                    }

                if (updated > 0) {
                    val updatedRow = Flags
                        .selectAll().where {
                            (Flags.projectId eq projectId) and
                                    (Flags.key eq key)
                        }
                        .firstOrNull()

                    updatedRow?.let { row ->
                        FlagResponse(
                            key = row[Flags.key],
                            type = row[Flags.type],
                            value = row[Flags.value] ?: "",
                            description = row[Flags.description],
                            isEnabled = row[Flags.isEnabled],
                            createdAt = row[Flags.createdAt].epochSeconds * 1000,
                            updatedAt = row[Flags.updatedAt].epochSeconds * 1000
                        )
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                logger.error("Error toggling flag (projectId=$projectId, key=$key)", e)
                throw e
            }
        }
    }
}

