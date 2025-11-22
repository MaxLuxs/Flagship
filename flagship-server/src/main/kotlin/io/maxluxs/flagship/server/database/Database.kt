package io.maxluxs.flagship.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.maxluxs.flagship.server.database.models.ApiKeys
import io.maxluxs.flagship.server.database.models.AuditLogs
import io.maxluxs.flagship.server.database.models.Experiments
import io.maxluxs.flagship.server.database.models.Flags
import io.maxluxs.flagship.server.database.models.ProjectMembers
import io.maxluxs.flagship.server.database.models.Projects
import io.maxluxs.flagship.server.database.models.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseConfig {
    fun connect(
        url: String = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/flagship",
        user: String = System.getenv("DATABASE_USER") ?: "flagship",
        password: String = System.getenv("DATABASE_PASSWORD") ?: "flagship_dev_password",
        driver: String = "org.postgresql.Driver"
    ) {
        val config = HikariConfig().apply {
            jdbcUrl = url
            username = user
            this.password = password
            driverClassName = driver
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        // Create tables
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                Projects,
                ProjectMembers,
                ApiKeys,
                Flags,
                Experiments,
                AuditLogs
            )
        }
    }
}

