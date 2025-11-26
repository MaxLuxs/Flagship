package io.maxluxs.flagship.server.health

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.maxluxs.flagship.server.database.DatabaseConfig
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: Long = System.currentTimeMillis(),
    val checks: Map<String, CheckStatus> = emptyMap()
)

@Serializable
data class CheckStatus(
    val status: String,
    val message: String? = null
)

fun Routing.healthRoutes() {
    get("/health") {
        val dbHealthy = DatabaseConfig.healthCheck()
        val poolStats = DatabaseConfig.getConnectionPoolStats()
        
        val checks = mapOf(
            "database" to CheckStatus(
                status = if (dbHealthy) "healthy" else "unhealthy",
                message = if (dbHealthy) null else "Database connection failed"
            ),
            "connectionPool" to CheckStatus(
                status = if (poolStats != null) "healthy" else "unknown",
                message = poolStats?.let { 
                    "Active: ${it.active}, Idle: ${it.idle}, Total: ${it.total}"
                }
            )
        )
        
        val allHealthy = checks.values.all { it.status == "healthy" }
        val status = if (allHealthy) "healthy" else "degraded"
        
        val statusCode = if (allHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        
        call.respond(statusCode, HealthResponse(
            status = status,
            checks = checks
        ))
    }
    
    get("/health/liveness") {
        // Simple liveness check - server is running
        call.respond(HttpStatusCode.OK, HealthResponse(
            status = "alive"
        ))
    }
    
    get("/health/readiness") {
        // Readiness check - server is ready to accept traffic
        val dbHealthy = DatabaseConfig.healthCheck()
        
        if (dbHealthy) {
            call.respond(HttpStatusCode.OK, HealthResponse(
                status = "ready"
            ))
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable, HealthResponse(
                status = "not_ready",
                checks = mapOf(
                    "database" to CheckStatus(
                        status = "unhealthy",
                        message = "Database not available"
                    )
                )
            ))
        }
    }
}

