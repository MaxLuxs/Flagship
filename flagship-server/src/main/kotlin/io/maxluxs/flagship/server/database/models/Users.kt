package io.maxluxs.flagship.server.database.models

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object Users : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val name = varchar("name", 255).nullable()
    val isAdmin = bool("is_admin").default(false)

    @OptIn(ExperimentalTime::class)
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }

    @OptIn(ExperimentalTime::class)
    val updatedAt = timestamp("updated_at").clientDefault { Clock.System.now() }
}

