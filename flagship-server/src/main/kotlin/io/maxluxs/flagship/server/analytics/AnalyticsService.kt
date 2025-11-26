package io.maxluxs.flagship.server.analytics

import io.maxluxs.flagship.shared.api.AnalyticsEvent
import io.maxluxs.flagship.shared.api.FlagStats
import io.maxluxs.flagship.shared.api.ExperimentStats
import io.maxluxs.flagship.shared.api.AnalyticsOverview
import io.maxluxs.flagship.server.database.models.AnalyticsEvents
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AnalyticsService {
    private val logger = LoggerFactory.getLogger(AnalyticsService::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalTime::class)
    suspend fun recordEvent(event: AnalyticsEvent) {
        newSuspendedTransaction {
            try {
                AnalyticsEvents.insert {
                    it[AnalyticsEvents.projectId] = UUID.fromString(event.projectId)
                    it[AnalyticsEvents.eventType] = event.eventType
                    it[AnalyticsEvents.entityType] = event.entityType
                    it[AnalyticsEvents.entityId] = event.entityId
                    it[AnalyticsEvents.userId] = event.userId?.let { UUID.fromString(it) }
                    it[AnalyticsEvents.attributes] = if (event.attributes.isNotEmpty()) {
                        json.encodeToString(
                            kotlinx.serialization.json.JsonObject.serializer(),
                            kotlinx.serialization.json.buildJsonObject {
                                event.attributes.forEach { (k, v) -> put(k, v) }
                            })
                    } else null
                    it[AnalyticsEvents.timestamp] = Clock.System.now()
                }
            } catch (e: Exception) {
                logger.error("Error recording analytics event", e)
            }
        }
    }

    suspend fun getFlagStats(
        projectId: UUID,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<FlagStats> {
        return newSuspendedTransaction {
            val query = AnalyticsEvents
                .selectAll().where {
                    (AnalyticsEvents.projectId eq projectId) and
                            (AnalyticsEvents.entityType eq "flag")
                }

            val events = query.map { row ->
                row[AnalyticsEvents.entityId] to row[AnalyticsEvents.eventType]
            }

            val statsByFlag = events.groupBy { it.first }
                .map { (flagKey, events) ->
                    val enabledCount = events.count { it.second == "flag_enabled" }
                    val disabledCount = events.count { it.second == "flag_disabled" }
                    FlagStats(
                        flagKey = flagKey ?: "",
                        enabledCount = enabledCount.toLong(),
                        disabledCount = disabledCount.toLong(),
                        totalRequests = events.size.toLong()
                    )
                }

            statsByFlag
        }
    }

    suspend fun getExperimentStats(
        projectId: UUID,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<ExperimentStats> {
        return newSuspendedTransaction {
            val query = AnalyticsEvents
                .selectAll().where {
                    (AnalyticsEvents.projectId eq projectId) and
                            (AnalyticsEvents.entityType eq "experiment")
                }

            val events = query.map { row ->
                val attributes = row[AnalyticsEvents.attributes]
                val variant = try {
                    if (attributes != null) {
                        val jsonObj = json.parseToJsonElement(attributes).jsonObject
                        jsonObj["variant"]?.jsonPrimitive?.content ?: "unknown"
                    } else {
                        "unknown"
                    }
                } catch (e: Exception) {
                    "unknown"
                }
                row[AnalyticsEvents.entityId] to variant
            }

            val statsByExperiment = events.groupBy { it.first }
                .map { (experimentKey, variants) ->
                    val distribution = variants.groupingBy { it.second }.eachCount()
                        .mapValues { it.value.toLong() }
                    ExperimentStats(
                        experimentKey = experimentKey ?: "",
                        variantDistribution = distribution,
                        totalAssignments = variants.size.toLong()
                    )
                }

            statsByExperiment
        }
    }

    suspend fun getOverview(projectId: UUID, period: String = "24h"): AnalyticsOverview {
        val endTime = System.currentTimeMillis()
        val startTime = when (period) {
            "1h" -> endTime - 3600_000
            "24h" -> endTime - 86400_000
            "7d" -> endTime - 604800_000
            "30d" -> endTime - 2592000_000
            else -> endTime - 86400_000
        }

        val flagStats = getFlagStats(projectId, startTime, endTime)
        val experimentStats = getExperimentStats(projectId, startTime, endTime)

        val totalEvents = newSuspendedTransaction {
            AnalyticsEvents
                .selectAll().where { AnalyticsEvents.projectId eq projectId }
                .count()
        }

        return AnalyticsOverview(
            projectId = projectId.toString(),
            totalFlags = flagStats.size,
            totalExperiments = experimentStats.size,
            totalEvents = totalEvents,
            flagStats = flagStats,
            experimentStats = experimentStats,
            period = period
        )
    }
}

