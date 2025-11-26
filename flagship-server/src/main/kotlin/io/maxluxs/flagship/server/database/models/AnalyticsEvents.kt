package io.maxluxs.flagship.server.database.models

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object AnalyticsEvents : UUIDTable("analytics_events") {
    val projectId = uuid("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val eventType =
        varchar("event_type", 100) // flag_enabled, flag_disabled, experiment_assigned, conversion
    val entityType = varchar("entity_type", 50).nullable() // flag, experiment
    val entityId = varchar("entity_id", 255).nullable() // flag key, experiment key
    val userId =
        uuid("user_id").references(Users.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val attributes = text("attributes").nullable() // JSON with additional data
    val timestamp = timestamp("timestamp").default(Clock.System.now())

    init {
        index(isUnique = false, projectId, timestamp)
        index(isUnique = false, eventType)
        index(isUnique = false, entityType, entityId)
    }
}

