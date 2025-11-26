package io.maxluxs.flagship.server.database.models

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object ProviderMetrics : UUIDTable("provider_metrics") {
    val projectId = uuid("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val providerName = varchar("provider_name", 255)
    val totalRequests = long("total_requests").default(0L)
    val successfulRequests = long("successful_requests").default(0L)
    val failedRequests = long("failed_requests").default(0L)
    val averageResponseTimeMs = double("average_response_time_ms").default(0.0)
    val lastRequestTimeMs = long("last_request_time_ms").nullable()
    val lastSuccessTimeMs = long("last_success_time_ms").nullable()
    val lastFailureTimeMs = long("last_failure_time_ms").nullable()
    val consecutiveFailures = integer("consecutive_failures").default(0)
    val timestamp = timestamp("timestamp").clientDefault { Clock.System.now() }

    init {
        index(isUnique = false, projectId, providerName, timestamp)
    }
}

