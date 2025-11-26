package io.maxluxs.flagship.server.analytics

import io.maxluxs.flagship.shared.api.ProviderMetricsData
import io.maxluxs.flagship.shared.api.ProviderHealthStatus
import io.maxluxs.flagship.shared.api.ProviderMetricsRequest
import io.maxluxs.flagship.server.database.models.ProviderMetrics
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ProviderAnalyticsService {
    private val logger = LoggerFactory.getLogger(ProviderAnalyticsService::class.java)

    suspend fun recordMetrics(projectId: UUID, metrics: ProviderMetricsRequest) {
        newSuspendedTransaction {
            try {
                ProviderMetrics.insert {
                    it[ProviderMetrics.projectId] = projectId
                    it[ProviderMetrics.providerName] = metrics.providerName
                    it[ProviderMetrics.totalRequests] = metrics.totalRequests
                    it[ProviderMetrics.successfulRequests] = metrics.successfulRequests
                    it[ProviderMetrics.failedRequests] = metrics.failedRequests
                    it[ProviderMetrics.averageResponseTimeMs] = metrics.averageResponseTimeMs
                    it[ProviderMetrics.lastRequestTimeMs] = metrics.lastRequestTimeMs
                    it[ProviderMetrics.lastSuccessTimeMs] = metrics.lastSuccessTimeMs
                    it[ProviderMetrics.lastFailureTimeMs] = metrics.lastFailureTimeMs
                    it[ProviderMetrics.consecutiveFailures] = metrics.consecutiveFailures
                }
            } catch (e: Exception) {
                logger.error("Error recording provider metrics", e)
                throw e
            }
        }
    }

    suspend fun getLatestMetrics(
        projectId: UUID,
        providerName: String? = null
    ): List<ProviderMetricsData> {
        return newSuspendedTransaction {
            try {
                val query = ProviderMetrics
                    .selectAll().where { ProviderMetrics.projectId eq projectId }
                    .let { q ->
                        if (providerName != null) {
                            q.andWhere { ProviderMetrics.providerName eq providerName }
                        } else {
                            q
                        }
                    }
                    .orderBy(ProviderMetrics.timestamp to SortOrder.DESC)
                    .limit(100)

                query.map { row ->
                    ProviderMetricsData(
                        providerName = row[ProviderMetrics.providerName],
                        totalRequests = row[ProviderMetrics.totalRequests],
                        successfulRequests = row[ProviderMetrics.successfulRequests],
                        failedRequests = row[ProviderMetrics.failedRequests],
                        averageResponseTimeMs = row[ProviderMetrics.averageResponseTimeMs],
                        lastRequestTimeMs = row[ProviderMetrics.lastRequestTimeMs],
                        lastSuccessTimeMs = row[ProviderMetrics.lastSuccessTimeMs],
                        lastFailureTimeMs = row[ProviderMetrics.lastFailureTimeMs],
                        consecutiveFailures = row[ProviderMetrics.consecutiveFailures],
                        timestamp = row[ProviderMetrics.timestamp].toEpochMilliseconds()
                    )
                }
            } catch (e: Exception) {
                logger.error("Error getting provider metrics", e)
                throw e
            }
        }
    }

    suspend fun getProviderHealthStatus(projectId: UUID): List<ProviderHealthStatus> {
        return newSuspendedTransaction {
            try {
                // Get all providers for this project
                val allProviders = ProviderMetrics
                    .selectAll().where { ProviderMetrics.projectId eq projectId }
                    .map { it[ProviderMetrics.providerName] }
                    .distinct()

                // Get latest metrics for each provider
                val latestMetrics = allProviders.mapNotNull { providerName ->
                    ProviderMetrics
                        .selectAll().where {
                            (ProviderMetrics.projectId eq projectId) and
                                    (ProviderMetrics.providerName eq providerName)
                        }
                        .orderBy(ProviderMetrics.timestamp to SortOrder.DESC)
                        .limit(1)
                        .firstOrNull()
                }

                latestMetrics.map { row ->
                    val totalRequests = row[ProviderMetrics.totalRequests]
                    val successfulRequests = row[ProviderMetrics.successfulRequests]
                    val successRate =
                        if (totalRequests > 0) successfulRequests.toDouble() / totalRequests else 0.0

                    ProviderHealthStatus(
                        providerName = row[ProviderMetrics.providerName],
                        isHealthy = row[ProviderMetrics.consecutiveFailures] < 5 && successRate > 0.8,
                        successRate = successRate,
                        consecutiveFailures = row[ProviderMetrics.consecutiveFailures],
                        lastSuccessTimeMs = row[ProviderMetrics.lastSuccessTimeMs],
                        averageResponseTimeMs = row[ProviderMetrics.averageResponseTimeMs]
                    )
                }
            } catch (e: Exception) {
                logger.error("Error getting provider health status", e)
                throw e
            }
        }
    }

    suspend fun getProviderMetricsHistory(
        projectId: UUID,
        providerName: String,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<ProviderMetricsData> {
        return newSuspendedTransaction {
            try {
                val query = ProviderMetrics
                    .selectAll().where {
                        (ProviderMetrics.projectId eq projectId) and
                                (ProviderMetrics.providerName eq providerName)
                    }

                query
                    .orderBy(ProviderMetrics.timestamp to SortOrder.ASC)
                    .mapNotNull { row ->
                        val rowTimestamp = row[ProviderMetrics.timestamp].toEpochMilliseconds()
                        // Filter by time range in memory if provided
                        if (startTime != null && rowTimestamp < startTime) return@mapNotNull null
                        if (endTime != null && rowTimestamp > endTime) return@mapNotNull null

                        ProviderMetricsData(
                            providerName = row[ProviderMetrics.providerName],
                            totalRequests = row[ProviderMetrics.totalRequests],
                            successfulRequests = row[ProviderMetrics.successfulRequests],
                            failedRequests = row[ProviderMetrics.failedRequests],
                            averageResponseTimeMs = row[ProviderMetrics.averageResponseTimeMs],
                            lastRequestTimeMs = row[ProviderMetrics.lastRequestTimeMs],
                            lastSuccessTimeMs = row[ProviderMetrics.lastSuccessTimeMs],
                            lastFailureTimeMs = row[ProviderMetrics.lastFailureTimeMs],
                            consecutiveFailures = row[ProviderMetrics.consecutiveFailures],
                            timestamp = row[ProviderMetrics.timestamp].toEpochMilliseconds()
                        )
                    }
            } catch (e: Exception) {
                logger.error("Error getting provider metrics history", e)
                throw e
            }
        }
    }
}

