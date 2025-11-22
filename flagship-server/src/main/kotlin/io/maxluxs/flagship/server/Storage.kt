package io.maxluxs.flagship.server

import io.maxluxs.flagship.provider.rest.*
import java.util.*

interface Storage {
    suspend fun getAllFlags(projectId: UUID): Map<String, RestFlagValue>
    suspend fun getFlag(projectId: UUID, key: String): RestFlagValue?
    suspend fun createFlag(projectId: UUID, key: String, flag: RestFlagValue, userId: UUID? = null): RestFlagValue
    suspend fun updateFlag(projectId: UUID, key: String, flag: RestFlagValue): RestFlagValue?
    suspend fun deleteFlag(projectId: UUID, key: String): Boolean

    suspend fun getAllExperiments(projectId: UUID): Map<String, RestExperiment>
    suspend fun getExperiment(projectId: UUID, key: String): RestExperiment?
    suspend fun createExperiment(projectId: UUID, key: String, experiment: RestExperiment, userId: UUID? = null): RestExperiment
    suspend fun updateExperiment(projectId: UUID, key: String, experiment: RestExperiment): RestExperiment?
    suspend fun deleteExperiment(projectId: UUID, key: String): Boolean

    suspend fun getConfig(projectId: UUID, revision: String? = null): RestResponse
    fun generateRevision(): String
}

