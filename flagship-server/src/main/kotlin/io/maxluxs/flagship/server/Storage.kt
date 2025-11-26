package io.maxluxs.flagship.server

import io.maxluxs.flagship.shared.api.FlagResponse
import io.maxluxs.flagship.shared.api.RestExperiment
import io.maxluxs.flagship.shared.api.RestFlagValue
import io.maxluxs.flagship.shared.api.RestResponse
import java.util.*

data class FlagMetadata(
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: UUID?
)

data class ExperimentMetadata(
    val createdAt: Long,
    val updatedAt: Long,
    val createdBy: UUID?,
    val isActive: Boolean
)

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
    
    // Extended methods for existence checks and metadata
    suspend fun flagExists(projectId: UUID, key: String): Boolean
    suspend fun experimentExists(projectId: UUID, key: String): Boolean
    suspend fun getFlagMetadata(projectId: UUID, key: String): FlagMetadata?
    suspend fun getExperimentMetadata(projectId: UUID, key: String): ExperimentMetadata?
    
    // Detailed flag methods
    suspend fun getAllFlagsDetailed(projectId: UUID): List<FlagResponse>
    suspend fun toggleFlag(projectId: UUID, key: String): FlagResponse?
}

