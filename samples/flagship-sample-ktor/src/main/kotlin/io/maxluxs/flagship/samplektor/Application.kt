package io.maxluxs.flagship.samplektor

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.maxluxs.flagship.ktor.flagship
import io.maxluxs.flagship.samplektor.routes.demoRoutes
import io.maxluxs.flagship.samplektor.routes.experimentsRoutes
import io.maxluxs.flagship.samplektor.routes.flagsRoutes
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main(args: Array<String>) {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val host = System.getenv("HOST") ?: "0.0.0.0"

    embeddedServer(Netty, port = port, host = host, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Configure serialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    // Configure logging
    install(CallLogging) {
        level = Level.INFO
    }

    // Install Flagship plugin with mock provider
    flagship {
        appKey = "sample-ktor-app"
        environment = "development"
        providers = listOf(MockFlagsProvider())
    }

    // Configure routes
    routing {
        flagsRoutes()
        experimentsRoutes()
        demoRoutes()
    }
}

