package io.maxluxs.flagship.server.database.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import kotlinx.datetime.Clock

object Projects : UUIDTable("projects") {
    val name = varchar("name", 255)
    val slug = varchar("slug", 255).uniqueIndex()
    val description = text("description").nullable()
    val ownerId = uuid("owner_id").references(Users.id)
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
}

