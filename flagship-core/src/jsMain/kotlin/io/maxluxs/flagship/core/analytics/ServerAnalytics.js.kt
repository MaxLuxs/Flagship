package io.maxluxs.flagship.core.analytics

import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

suspend fun ServerAnalytics.sendEventsActual(
    analyticsUrl: String,
    projectId: String,
    apiKey: String?,
    events: List<AnalyticsEvent>
) {
    val client = HttpClient(Js) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    try {
        val url = "$analyticsUrl/api/projects/$projectId/analytics/events"
        for (event in events) {
            val request = mapOf(
                "eventType" to event.eventType,
                "entityType" to event.entityType,
                "entityId" to event.entityId,
                "attributes" to event.attributes
            )
            
            client.post(url) {
                contentType(ContentType.Application.Json)
                apiKey?.let { header("Authorization", "Bearer $it") }
                setBody(request)
            }
        }
    } finally {
        client.close()
    }
}

