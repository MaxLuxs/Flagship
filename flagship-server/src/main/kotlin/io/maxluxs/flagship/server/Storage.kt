package io.maxluxs.flagship.server

import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.provider.rest.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

interface FlagStorage {
    suspend fun getAllFlags(): Map<String, RestFlagValue>
    suspend fun getFlag(key: String): RestFlagValue?
    suspend fun createFlag(key: String, flag: RestFlagValue): RestFlagValue
    suspend fun updateFlag(key: String, flag: RestFlagValue): RestFlagValue?
    suspend fun deleteFlag(key: String): Boolean

    suspend fun getAllExperiments(): Map<String, RestExperiment>
    suspend fun getExperiment(key: String): RestExperiment?
    suspend fun createExperiment(key: String, experiment: RestExperiment): RestExperiment
    suspend fun updateExperiment(key: String, experiment: RestExperiment): RestExperiment?
    suspend fun deleteExperiment(key: String): Boolean

    suspend fun getConfig(revision: String? = null): RestResponse
    fun generateRevision(): String
}

class InMemoryFlagStorage : FlagStorage {
    private val flags = mutableMapOf<String, RestFlagValue>()
    private val experiments = mutableMapOf<String, RestExperiment>()
    private val mutex = Mutex()
    private var currentRevision: String = UUID.randomUUID().toString()

    override suspend fun getAllFlags(): Map<String, RestFlagValue> = mutex.withLock {
        flags.toMap()
    }

    override suspend fun getFlag(key: String): RestFlagValue? = mutex.withLock {
        flags[key]
    }

    override suspend fun createFlag(key: String, flag: RestFlagValue): RestFlagValue =
        mutex.withLock {
            if (flags.containsKey(key)) {
                throw IllegalArgumentException("Flag with key '$key' already exists")
            }
            flags[key] = flag
            currentRevision = generateRevision()
            flag
        }

    override suspend fun updateFlag(key: String, flag: RestFlagValue): RestFlagValue? =
        mutex.withLock {
            if (flags.containsKey(key)) {
                flags[key] = flag
                currentRevision = generateRevision()
                flag
            } else {
                null
            }
        }

    override suspend fun deleteFlag(key: String): Boolean = mutex.withLock {
        val removed = flags.remove(key) != null
        if (removed) {
            currentRevision = generateRevision()
        }
        removed
    }

    override suspend fun getAllExperiments(): Map<String, RestExperiment> = mutex.withLock {
        experiments.toMap()
    }

    override suspend fun getExperiment(key: String): RestExperiment? = mutex.withLock {
        experiments[key]
    }

    override suspend fun createExperiment(key: String, experiment: RestExperiment): RestExperiment =
        mutex.withLock {
            if (experiments.containsKey(key)) {
                throw IllegalArgumentException("Experiment with key '$key' already exists")
            }
            experiments[key] = experiment
            currentRevision = generateRevision()
            experiment
        }

    override suspend fun updateExperiment(
        key: String,
        experiment: RestExperiment
    ): RestExperiment? = mutex.withLock {
        if (experiments.containsKey(key)) {
            experiments[key] = experiment
            currentRevision = generateRevision()
            experiment
        } else {
            null
        }
    }

    override suspend fun deleteExperiment(key: String): Boolean = mutex.withLock {
        val removed = experiments.remove(key) != null
        if (removed) {
            currentRevision = generateRevision()
        }
        removed
    }

    override suspend fun getConfig(revision: String?): RestResponse {
        val snapshot = mutex.withLock {
            RestResponse(
                revision = currentRevision,
                fetchedAt = System.currentTimeMillis(),
                ttlMs = 900_000L, // 15 minutes
                flags = flags.toMap(),
                experiments = experiments.toMap()
            )
        }

        // If revision matches, return empty response (no changes)
        if (revision == snapshot.revision) {
            return snapshot.copy(
                flags = emptyMap(),
                experiments = emptyMap()
            )
        }

        return snapshot
    }

    override fun generateRevision(): String = UUID.randomUUID().toString()
}

