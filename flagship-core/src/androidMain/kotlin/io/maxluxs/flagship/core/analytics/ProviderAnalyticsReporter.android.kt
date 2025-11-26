package io.maxluxs.flagship.core.analytics

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

suspend fun ProviderAnalyticsReporter.sendMetricsActual(
    analyticsUrl: String,
    projectId: String,
    apiKey: String?,
    metrics: List<ProviderMetricsRequest>
) {
    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    try {
        for (metric in metrics) {
            val url = "$analyticsUrl/api/projects/$projectId/analytics/providers"
            client.post(url) {
                contentType(ContentType.Application.Json)
                apiKey?.let { header("Authorization", "Bearer $it") }
                setBody(metric)
            }
        }
    } finally {
        client.close()
    }
}

