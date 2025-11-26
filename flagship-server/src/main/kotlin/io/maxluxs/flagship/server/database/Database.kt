package io.maxluxs.flagship.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.maxluxs.flagship.server.database.models.AnalyticsEvents
import io.maxluxs.flagship.server.database.models.ApiKeys
import io.maxluxs.flagship.server.database.models.AuditLogs
import io.maxluxs.flagship.server.database.models.Experiments
import io.maxluxs.flagship.server.database.models.Flags
import io.maxluxs.flagship.server.database.models.ProjectMembers
import io.maxluxs.flagship.server.database.models.Projects
import io.maxluxs.flagship.server.database.models.ProviderMetrics
import io.maxluxs.flagship.server.database.models.Users
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseConfig {
    private val logger = LoggerFactory.getLogger(DatabaseConfig::class.java)
    private var dataSource: HikariDataSource? = null

    fun connect(
        url: String = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/flagship",
        user: String = System.getenv("DATABASE_USER") ?: "flagship",
        password: String = System.getenv("DATABASE_PASSWORD") ?: "flagship_dev_password",
        driver: String = "org.postgresql.Driver"
    ) {
        try {
            val config = HikariConfig().apply {
                jdbcUrl = url
                username = user
                this.password = password
                driverClassName = driver
                maximumPoolSize = System.getenv("DB_POOL_MAX_SIZE")?.toIntOrNull() ?: 10
                minimumIdle = System.getenv("DB_POOL_MIN_IDLE")?.toIntOrNull() ?: 2
                connectionTimeout = System.getenv("DB_CONNECTION_TIMEOUT")?.toLongOrNull() ?: 30000
                idleTimeout = System.getenv("DB_IDLE_TIMEOUT")?.toLongOrNull() ?: 600000
                maxLifetime = System.getenv("DB_MAX_LIFETIME")?.toLongOrNull() ?: 1800000

                // SSL configuration for production
                val useSsl = System.getenv("DB_SSL")?.toBoolean() ?: false
                if (useSsl) {
                    addDataSourceProperty("ssl", "true")
                    addDataSourceProperty("sslmode", System.getenv("DB_SSLMODE") ?: "require")
                    val sslCert = System.getenv("DB_SSL_CERT")
                    val sslKey = System.getenv("DB_SSL_KEY")
                    val sslRootCert = System.getenv("DB_SSL_ROOT_CERT")
                    if (sslCert != null) addDataSourceProperty("sslcert", sslCert)
                    if (sslKey != null) addDataSourceProperty("sslkey", sslKey)
                    if (sslRootCert != null) addDataSourceProperty("sslrootcert", sslRootCert)
                }

                // Connection validation
                connectionTestQuery = "SELECT 1"
                validationTimeout = 5000

                // Leak detection
                leakDetectionThreshold =
                    System.getenv("DB_LEAK_DETECTION_THRESHOLD")?.toLongOrNull() ?: 60000

                // Logging - MBeans registration
                // registerMbeans property is not available in this HikariCP version
            }

            dataSource = HikariDataSource(config)
            Database.connect(dataSource!!)

            // Test connection
            transaction {
                exec("SELECT 1")
            }
            logger.info("Database connection established successfully")

            // Create tables
            transaction {
                try {
                    SchemaUtils.createMissingTablesAndColumns(
                        Users,
                        Projects,
                        ProjectMembers,
                        ApiKeys,
                        Flags,
                        Experiments,
                        AuditLogs,
                        AnalyticsEvents,
                        ProviderMetrics
                    )
                    logger.info("Database schema initialized successfully")
                } catch (e: Exception) {
                    logger.error("Error initializing database schema", e)
                    throw e
                }
            }

            // Run migrations
            try {
                Migrations.runMigrations()
            } catch (e: Exception) {
                logger.error("Error running migrations", e)
                throw e
            }
        } catch (e: Exception) {
            logger.error("Failed to connect to database", e)
            throw RuntimeException("Database connection failed", e)
        }
    }

    fun healthCheck(): Boolean {
        return try {
            dataSource?.connection?.use { conn ->
                conn.isValid(5)
            } ?: false
        } catch (e: Exception) {
            logger.warn("Database health check failed", e)
            false
        }
    }

    fun getConnectionPoolStats(): ConnectionPoolStats? {
        return dataSource?.let { ds ->
            val pool = ds.hikariPoolMXBean
            ConnectionPoolStats(
                active = pool.activeConnections,
                idle = pool.idleConnections,
                total = pool.totalConnections,
                threadsAwaitingConnection = pool.threadsAwaitingConnection,
                maxPoolSize = ds.maximumPoolSize,
                minIdle = ds.minimumIdle
            )
        }
    }

    data class ConnectionPoolStats(
        val active: Int,
        val idle: Int,
        val total: Int,
        val threadsAwaitingConnection: Int,
        val maxPoolSize: Int,
        val minIdle: Int
    )
}

