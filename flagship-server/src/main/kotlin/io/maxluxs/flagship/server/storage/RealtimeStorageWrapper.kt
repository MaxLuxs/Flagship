package io.maxluxs.flagship.server.storage

import io.maxluxs.flagship.server.Storage
import io.maxluxs.flagship.server.realtime.RealtimeService
import io.maxluxs.flagship.server.realtime.RealtimeEvent
import io.maxluxs.flagship.shared.api.RestExperiment
import io.maxluxs.flagship.shared.api.RestFlagValue
import io.maxluxs.flagship.shared.api.RestResponse
import java.util.*

class RealtimeStorageWrapper(
    private val delegate: Storage,
    private val realtimeService: RealtimeService
) : Storage by delegate {
    
    override suspend fun createFlag(projectId: UUID, key: String, flag: RestFlagValue, userId: UUID?): RestFlagValue {
        val result = delegate.createFlag(projectId, key, flag, userId)
        realtimeService.broadcast(
            RealtimeEvent(
                type = "flag_created",
                projectId = projectId.toString(),
                entityType = "flag",
                entityId = key,
                data = mapOf("key" to key, "type" to flag.type)
            )
        )
        return result
    }
    
    override suspend fun updateFlag(projectId: UUID, key: String, flag: RestFlagValue): RestFlagValue? {
        val result = delegate.updateFlag(projectId, key, flag)
        if (result != null) {
            realtimeService.broadcast(
                RealtimeEvent(
                    type = "flag_updated",
                    projectId = projectId.toString(),
                    entityType = "flag",
                    entityId = key,
                    data = mapOf("key" to key, "type" to flag.type)
                )
            )
        }
        return result
    }
    
    override suspend fun deleteFlag(projectId: UUID, key: String): Boolean {
        val result = delegate.deleteFlag(projectId, key)
        if (result) {
            realtimeService.broadcast(
                RealtimeEvent(
                    type = "flag_deleted",
                    projectId = projectId.toString(),
                    entityType = "flag",
                    entityId = key,
                    data = mapOf("key" to key)
                )
            )
        }
        return result
    }
    
    override suspend fun createExperiment(projectId: UUID, key: String, experiment: RestExperiment, userId: UUID?): RestExperiment {
        val result = delegate.createExperiment(projectId, key, experiment, userId)
        realtimeService.broadcast(
            RealtimeEvent(
                type = "experiment_created",
                projectId = projectId.toString(),
                entityType = "experiment",
                entityId = key
            )
        )
        return result
    }
    
    override suspend fun updateExperiment(projectId: UUID, key: String, experiment: RestExperiment): RestExperiment? {
        val result = delegate.updateExperiment(projectId, key, experiment)
        if (result != null) {
            realtimeService.broadcast(
                RealtimeEvent(
                    type = "experiment_updated",
                    projectId = projectId.toString(),
                    entityType = "experiment",
                    entityId = key
                )
            )
        }
        return result
    }
    
    override suspend fun deleteExperiment(projectId: UUID, key: String): Boolean {
        val result = delegate.deleteExperiment(projectId, key)
        if (result) {
            realtimeService.broadcast(
                RealtimeEvent(
                    type = "experiment_deleted",
                    projectId = projectId.toString(),
                    entityType = "experiment",
                    entityId = key
                )
            )
        }
        return result
    }
}

