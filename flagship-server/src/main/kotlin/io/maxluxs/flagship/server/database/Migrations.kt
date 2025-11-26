package io.maxluxs.flagship.server.database

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object Migrations {
    private val logger = LoggerFactory.getLogger(Migrations::class.java)

    private const val CURRENT_SCHEMA_VERSION = 1

    object SchemaVersion : IntIdTable("schema_version") {
        val version = integer("version").uniqueIndex()

        @OptIn(ExperimentalTime::class)
        val appliedAt = timestamp("applied_at").default(Clock.System.now())
    }

    fun runMigrations() {
        transaction {
            try {
                // Create schema_version table if it doesn't exist
                SchemaUtils.createMissingTablesAndColumns(SchemaVersion)

                // Get current version
                val currentVersion = SchemaVersion
                    .selectAll()
                    .maxByOrNull { it[SchemaVersion.version] }
                    ?.get(SchemaVersion.version) ?: 0

                logger.info("Current database schema version: $currentVersion")

                // Run migrations
                if (currentVersion < 1) {
                    migrateToVersion1()
                    setVersion(1)
                }

                logger.info("Database migrations completed. Schema version: $CURRENT_SCHEMA_VERSION")
            } catch (e: Exception) {
                logger.error("Error running migrations", e)
                throw e
            }
        }
    }

    private fun migrateToVersion1() {
        logger.info("Running migration to version 1: Creating initial schema")
        // Schema is created by SchemaUtils.createMissingTablesAndColumns in DatabaseConfig
        // This migration is for future schema changes
    }

    private fun setVersion(version: Int) {
        try {
            SchemaVersion.insert {
                it[SchemaVersion.version] = version
            }
        } catch (e: Exception) {
            // Version already exists, ignore
            logger.debug("Schema version $version already exists")
        }
    }

    fun getCurrentVersion(): Int {
        return transaction {
            try {
                SchemaVersion
                    .selectAll()
                    .maxByOrNull { it[SchemaVersion.version] }
                    ?.get(SchemaVersion.version) ?: 0
            } catch (e: Exception) {
                // Table doesn't exist yet
                0
            }
        }
    }
}

