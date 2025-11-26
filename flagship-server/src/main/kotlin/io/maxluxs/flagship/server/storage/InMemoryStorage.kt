package io.maxluxs.flagship.server.storage

import io.maxluxs.flagship.server.ExperimentMetadata
import io.maxluxs.flagship.server.FlagMetadata
import io.maxluxs.flagship.server.Storage
import io.maxluxs.flagship.shared.api.FlagResponse
import io.maxluxs.flagship.shared.api.RestExperiment
import io.maxluxs.flagship.shared.api.RestFlagValue
import io.maxluxs.flagship.shared.api.RestResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

class InMemoryStorage : Storage {
    private val flags = mutableMapOf<UUID, MutableMap<String, RestFlagValue>>()
    private val experiments = mutableMapOf<UUID, MutableMap<String, RestExperiment>>()
    private val mutex = Mutex()
    private var currentRevision: String = UUID.randomUUID().toString()

    override suspend fun getAllFlags(projectId: UUID): Map<String, RestFlagValue> = mutex.withLock {
        flags[projectId]?.toMap() ?: emptyMap()
    }

    override suspend fun getFlag(projectId: UUID, key: String): RestFlagValue? = mutex.withLock {
        flags[projectId]?.get(key)
    }

    override suspend fun createFlag(
        projectId: UUID,
        key: String,
        flag: RestFlagValue,
        userId: UUID?
    ): RestFlagValue =
        mutex.withLock {
            val projectFlags = flags.getOrPut(projectId) { mutableMapOf() }
            if (projectFlags.containsKey(key)) {
                throw IllegalArgumentException("Flag with key '$key' already exists")
            }
            projectFlags[key] = flag
            currentRevision = generateRevision()
            flag
        }

    override suspend fun updateFlag(
        projectId: UUID,
        key: String,
        flag: RestFlagValue
    ): RestFlagValue? =
        mutex.withLock {
            val projectFlags = flags[projectId]
            if (projectFlags != null && projectFlags.containsKey(key)) {
                projectFlags[key] = flag
                currentRevision = generateRevision()
                flag
            } else {
                null
            }
        }

    override suspend fun deleteFlag(projectId: UUID, key: String): Boolean = mutex.withLock {
        val projectFlags = flags[projectId]
        val removed = projectFlags?.remove(key) != null
        if (removed) {
            currentRevision = generateRevision()
        }
        removed
    }

    override suspend fun getAllExperiments(projectId: UUID): Map<String, RestExperiment> =
        mutex.withLock {
            experiments[projectId]?.toMap() ?: emptyMap()
        }

    override suspend fun getExperiment(projectId: UUID, key: String): RestExperiment? =
        mutex.withLock {
            experiments[projectId]?.get(key)
        }

    override suspend fun createExperiment(
        projectId: UUID,
        key: String,
        experiment: RestExperiment,
        userId: UUID?
    ): RestExperiment =
        mutex.withLock {
            val projectExperiments = experiments.getOrPut(projectId) { mutableMapOf() }
            if (projectExperiments.containsKey(key)) {
                throw IllegalArgumentException("Experiment with key '$key' already exists")
            }
            projectExperiments[key] = experiment
            currentRevision = generateRevision()
            experiment
        }

    override suspend fun updateExperiment(
        projectId: UUID,
        key: String,
        experiment: RestExperiment
    ): RestExperiment? =
        mutex.withLock {
            val projectExperiments = experiments[projectId]
            if (projectExperiments != null && projectExperiments.containsKey(key)) {
                projectExperiments[key] = experiment
                currentRevision = generateRevision()
                experiment
            } else {
                null
            }
        }

    override suspend fun deleteExperiment(projectId: UUID, key: String): Boolean = mutex.withLock {
        val projectExperiments = experiments[projectId]
        val removed = projectExperiments?.remove(key) != null
        if (removed) {
            currentRevision = generateRevision()
        }
        removed
    }

    override suspend fun getConfig(projectId: UUID, revision: String?): RestResponse {
        val snapshot = mutex.withLock {
            RestResponse(
                revision = currentRevision,
                fetchedAt = System.currentTimeMillis(),
                ttlMs = 900_000L,
                flags = flags[projectId]?.toMap() ?: emptyMap(),
                experiments = experiments[projectId]?.toMap() ?: emptyMap()
            )
        }

        if (revision == snapshot.revision) {
            return snapshot.copy(
                flags = emptyMap(),
                experiments = emptyMap()
            )
        }

        return snapshot
    }

    override fun generateRevision(): String = UUID.randomUUID().toString()

    override suspend fun flagExists(projectId: UUID, key: String): Boolean = mutex.withLock {
        flags[projectId]?.containsKey(key) ?: false
    }

    override suspend fun experimentExists(projectId: UUID, key: String): Boolean = mutex.withLock {
        experiments[projectId]?.containsKey(key) ?: false
    }

    override suspend fun getFlagMetadata(projectId: UUID, key: String): FlagMetadata? =
        mutex.withLock {
            if (flags[projectId]?.containsKey(key) == true) {
                FlagMetadata(
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    createdBy = null
                )
            } else {
                null
            }
        }

    override suspend fun getExperimentMetadata(projectId: UUID, key: String): ExperimentMetadata? =
        mutex.withLock {
            if (experiments[projectId]?.containsKey(key) == true) {
                ExperimentMetadata(
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    createdBy = null,
                    isActive = true
                )
            } else {
                null
            }
        }

    override suspend fun getAllFlagsDetailed(projectId: UUID): List<FlagResponse> = mutex.withLock {
        flags[projectId]?.map { (key, flag) ->
            FlagResponse(
                key = key,
                type = flag.type,
                value = flag.value.toString(),
                description = null,
                isEnabled = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        } ?: emptyList()
    }

    override suspend fun toggleFlag(projectId: UUID, key: String): FlagResponse? = mutex.withLock {
        val flag = flags[projectId]?.get(key) ?: return null
        // InMemoryStorage doesn't track enabled state, so just return the flag
        FlagResponse(
            key = key,
            type = flag.type,
            value = flag.value.toString(),
            description = null,
            isEnabled = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
}

