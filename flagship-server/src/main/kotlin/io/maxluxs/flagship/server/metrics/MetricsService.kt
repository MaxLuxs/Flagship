package io.maxluxs.flagship.server.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.Histogram
import io.prometheus.client.Summary

object MetricsService {
    // HTTP request metrics
    val httpRequestsTotal = Counter.build()
        .name("http_requests_total")
        .help("Total number of HTTP requests")
        .labelNames("method", "endpoint", "status")
        .register()
    
    val httpRequestDuration = Histogram.build()
        .name("http_request_duration_seconds")
        .help("HTTP request duration in seconds")
        .labelNames("method", "endpoint")
        .buckets(0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0)
        .register()
    
    // Database metrics
    val dbConnectionsActive = Gauge.build()
        .name("db_connections_active")
        .help("Number of active database connections")
        .register()
    
    val dbConnectionsIdle = Gauge.build()
        .name("db_connections_idle")
        .help("Number of idle database connections")
        .register()
    
    val dbQueriesTotal = Counter.build()
        .name("db_queries_total")
        .help("Total number of database queries")
        .labelNames("operation", "status")
        .register()
    
    val dbQueryDuration = Histogram.build()
        .name("db_query_duration_seconds")
        .help("Database query duration in seconds")
        .labelNames("operation")
        .buckets(0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0)
        .register()
    
    // Business metrics
    val flagsTotal = Gauge.build()
        .name("flags_total")
        .help("Total number of flags")
        .labelNames("project_id")
        .register()
    
    val experimentsTotal = Gauge.build()
        .name("experiments_total")
        .help("Total number of experiments")
        .labelNames("project_id")
        .register()
    
    val apiKeysTotal = Gauge.build()
        .name("api_keys_total")
        .help("Total number of API keys")
        .labelNames("project_id", "type")
        .register()
    
    val usersTotal = Gauge.build()
        .name("users_total")
        .help("Total number of users")
        .register()
    
    val projectsTotal = Gauge.build()
        .name("projects_total")
        .help("Total number of projects")
        .register()
    
    // Authentication metrics
    val authAttemptsTotal = Counter.build()
        .name("auth_attempts_total")
        .help("Total number of authentication attempts")
        .labelNames("result")
        .register()
    
    // Error metrics
    val errorsTotal = Counter.build()
        .name("errors_total")
        .help("Total number of errors")
        .labelNames("type", "endpoint")
        .register()
    
    fun updateConnectionPoolStats(active: Int, idle: Int) {
        dbConnectionsActive.set(active.toDouble())
        dbConnectionsIdle.set(idle.toDouble())
    }
}

