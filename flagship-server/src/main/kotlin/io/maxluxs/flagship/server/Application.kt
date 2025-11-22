package io.maxluxs.flagship.server

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing
import io.ktor.server.routing.get
import io.ktor.server.response.respondRedirect
import io.ktor.server.http.content.staticResources
import io.ktor.server.http.content.resources
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import io.ktor.http.HttpMethod
import io.ktor.http.HttpHeaders
import io.maxluxs.flagship.server.database.DatabaseConfig
import io.maxluxs.flagship.server.auth.AuthService
import io.maxluxs.flagship.server.auth.authRoutes
import io.maxluxs.flagship.server.auth.configureAuth
import io.maxluxs.flagship.server.storage.DatabaseStorage
import io.maxluxs.flagship.server.admin.adminRoutes
import io.maxluxs.flagship.server.admin.auditRoutes
import io.maxluxs.flagship.server.audit.AuditService
import io.maxluxs.flagship.server.routes.projectRoutes

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseConfig.connect()
    
    install(CallLogging) {
        level = Level.INFO
    }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        anyHost()
    }

    // Configure authentication
    val authService = AuthService()
    configureAuth(authService)

    // Use database storage
    val storage = DatabaseStorage()
    val auditService = AuditService()

    routing {
        // Public auth routes
        authRoutes(authService)
        
        // Admin routes (require auth)
        adminRoutes(auditService)
        auditRoutes(auditService)
        
        // Project routes (flags, experiments, config)
        projectRoutes(storage, auditService)
        
        // Landing page
        get("/") {
            call.respondRedirect("/landing/")
        }
        
        // Landing page static files
        staticResources("/landing", "landing")
        
        // Admin UI - redirect /admin to admin panel
        get("/admin") {
            call.respondRedirect("/admin/")
        }
        
        // Admin UI static files
        staticResources("/admin", "admin-ui")
    }
}

