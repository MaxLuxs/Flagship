package io.maxluxs.flagship.server.database.models

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object Projects : UUIDTable("projects") {
    val name = varchar("name", 255)
    val slug = varchar("slug", 255).uniqueIndex()
    val description = text("description").nullable()
    val ownerId = uuid("owner_id").references(Users.id)
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
}

