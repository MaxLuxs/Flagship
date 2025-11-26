package io.maxluxs.flagship.server

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.maxluxs.flagship.server.admin.adminRoutes
import io.maxluxs.flagship.server.analytics.AnalyticsService
import io.maxluxs.flagship.server.analytics.analyticsRoutes
import io.maxluxs.flagship.server.audit.AuditService
import io.maxluxs.flagship.server.auth.AuthService
import io.maxluxs.flagship.server.auth.ProjectAccessService
import io.maxluxs.flagship.server.auth.authRoutes
import io.maxluxs.flagship.server.auth.configureAuth
import io.maxluxs.flagship.server.health.healthRoutes
import io.maxluxs.flagship.server.metrics.metricsRoutes
import io.maxluxs.flagship.server.realtime.RealtimeService
import io.maxluxs.flagship.server.realtime.realtimeRoutes
import io.maxluxs.flagship.server.routes.projectRoutes
import io.maxluxs.flagship.server.storage.InMemoryStorage
import kotlinx.serialization.json.Json
import java.util.*

fun Application.testModule(
    storage: Storage = InMemoryStorage(),
    authService: AuthService = AuthService(),
    auditService: AuditService = AuditService(),
    analyticsService: AnalyticsService = AnalyticsService(),
    realtimeService: RealtimeService = RealtimeService(),
    accessService: Any = ProjectAccessService()
) {
    configureSerialization()
    configureAuth(authService)
    
    // Note: ProjectAccessService methods are not virtual, so we can't override them
    // For now, we'll use the real ProjectAccessService which requires DB
    // Tests that need mocking should set up a test database or use Testcontainers
    val projectAccessService = when (accessService) {
        is ProjectAccessService -> accessService
        else -> ProjectAccessService()
    }
    
    routing {
        healthRoutes()
        metricsRoutes()
        authRoutes(authService)
        adminRoutes(auditService, projectAccessService)
        projectRoutes(storage, auditService, projectAccessService)
        analyticsRoutes(analyticsService, projectAccessService)
        realtimeRoutes(realtimeService, projectAccessService)
    }
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}

fun withTestApplication(
    storage: Storage = InMemoryStorage(),
    authService: AuthService = AuthService(),
    auditService: AuditService = AuditService(),
    analyticsService: AnalyticsService = AnalyticsService(),
    realtimeService: RealtimeService = RealtimeService(),
    accessService: ProjectAccessService = ProjectAccessService(),
    block: suspend ApplicationTestBuilder.() -> Unit
): Unit = testApplication {
    application {
        testModule(storage, authService, auditService, analyticsService, realtimeService, accessService)
    }
    block()
}

fun generateTestToken(authService: AuthService, userId: UUID, email: String, isAdmin: Boolean = false): String {
    return authService.generateToken(userId, email, isAdmin)
}

fun HttpHeadersBuilder.withAuth(token: String) {
    set("Authorization", "Bearer $token")
}
