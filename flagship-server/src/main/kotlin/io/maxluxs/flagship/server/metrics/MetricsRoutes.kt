package io.maxluxs.flagship.server.metrics

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import java.io.StringWriter

fun Routing.metricsRoutes() {
    get("/metrics") {
        val registry = CollectorRegistry.defaultRegistry
        val writer = StringWriter()
        TextFormat.write004(writer, registry.metricFamilySamples())
        
        call.respondText(
            text = writer.toString(),
            contentType = io.ktor.http.ContentType.Text.Plain
        )
    }
}

