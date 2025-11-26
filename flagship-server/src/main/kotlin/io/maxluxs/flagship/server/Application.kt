package io.maxluxs.flagship.server

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.maxluxs.flagship.server.admin.adminRoutes
import io.maxluxs.flagship.server.analytics.AnalyticsService
import io.maxluxs.flagship.server.analytics.analyticsRoutes
import io.maxluxs.flagship.server.audit.AuditService
import io.maxluxs.flagship.server.auth.AuthService
import io.maxluxs.flagship.server.auth.ProjectAccessService
import io.maxluxs.flagship.server.auth.authRoutes
import io.maxluxs.flagship.server.auth.configureAuth
import io.maxluxs.flagship.server.database.DatabaseConfig
import io.maxluxs.flagship.server.health.healthRoutes
import io.maxluxs.flagship.server.metrics.metricsRoutes
import io.maxluxs.flagship.server.realtime.RealtimeService
import io.maxluxs.flagship.server.realtime.realtimeRoutes
import io.maxluxs.flagship.server.routes.projectRoutes
import io.maxluxs.flagship.server.storage.DatabaseStorage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val host = System.getenv("HOST") ?: "0.0.0.0"

    embeddedServer(Netty, port = port, host = host, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")

    // Initialize database
    try {
        DatabaseConfig.connect()
        logger.info("Database initialized successfully")
    } catch (e: Exception) {
        logger.error("Failed to initialize database", e)
        throw e
    }

    // Initialize services
    val storage: Storage = DatabaseStorage()
    val authService = AuthService()
    val auditService = AuditService()
    val analyticsService = AnalyticsService()
    val realtimeService = RealtimeService()
    val accessService = ProjectAccessService()

    // Configure plugins
    configureSerialization()
    configureCORS()
    configureLogging()
    configureDefaultHeaders()
    configureStatusPages()
    configureWebSockets()
    configureAuth(authService)

    routing {
        healthRoutes()
        metricsRoutes()
        authRoutes(authService)
        adminRoutes(auditService, accessService)
        projectRoutes(storage, auditService, accessService)
        analyticsRoutes(analyticsService, accessService)
        realtimeRoutes(realtimeService, accessService)
        staticResources("/admin", "admin-ui")
        staticResources("/", "landing")

        get("/admin/{...}") {
            val resource = javaClass.classLoader.getResourceAsStream("admin-ui/index.html")
            if (resource != null) {
                val content = resource.use { it.readBytes() }
                call.respondBytes(content, ContentType.Text.Html)
            } else {
                call.respond(HttpStatusCode.NotFound, "Admin UI not found")
            }
        }
    }

    logger.info(
        "Application started successfully on port ${
            environment.config.propertyOrNull("ktor.deployment.port")?.getString() ?: "8080"
        }"
    )
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

fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)

        val allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS")?.split(",")?.map { it.trim() }
            ?: listOf("*")

        if (allowedOrigins.contains("*")) {
            anyHost()
        } else {
            allowedOrigins.forEach { origin ->
                allowHost(origin, schemes = listOf("http", "https"))
            }
        }

        allowCredentials = true
    }
}

fun Application.configureLogging() {
    install(CallLogging) {
        level = Level.INFO
    }
}

fun Application.configureDefaultHeaders() {
    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("X-XSS-Protection", "1; mode=block")
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val logger = LoggerFactory.getLogger("StatusPages")
            logger.error("Unhandled exception", cause)

            when (cause) {
                is BadRequestException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (cause.message ?: "Bad request"))
                    )
                }

                is SerializationException -> {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid request format: ${cause.message}")
                    )
                }

                else -> {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Internal server error")
                    )
                }
            }
        }
    }
}

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}