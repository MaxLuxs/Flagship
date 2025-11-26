package io.maxluxs.flagship.provider.rest

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.provider.BaseFlagsProvider
import io.maxluxs.flagship.core.provider.RealtimeFlagsProvider
import io.maxluxs.flagship.core.util.SystemClock
import io.maxluxs.flagship.shared.api.RestResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * WebSocket provider for real-time flag updates from Flagship server.
 *
 * Connects to `/ws/events` endpoint and receives real-time updates when flags change.
 *
 * Example:
 * ```kotlin
 * val provider = WebSocketFlagsProvider(
 *     client = httpClient,
 *     baseUrl = "https://api.flagship.io",
 *     apiKey = "your-api-key",
 *     projectId = "project-id"
 * )
 *
 * val realtimeManager = RealtimeManager(flagsManager, scope)
 * realtimeManager.connect(provider)
 * ```
 */
class WebSocketFlagsProvider(
    private val client: HttpClient,
    private val baseUrl: String,
    private val apiKey: String,
    private val projectId: String,
    name: String = "websocket"
) : BaseFlagsProvider(name, clock = SystemClock), RealtimeFlagsProvider {

    private val json = Json { ignoreUnknownKeys = true }
    private var webSocketSession: DefaultWebSocketSession? = null
    private var isConnectedState = false

    protected override suspend fun fetchSnapshot(currentRevision: String?): ProviderSnapshot {
        // For WebSocket provider, we still need to fetch initial snapshot via REST
        // Use RestFlagsProvider but we need to add Authorization header manually
        // Since RestFlagsProvider doesn't support apiKey, we'll fetch directly
        val url = if (currentRevision != null) {
            "$baseUrl/config?rev=$currentRevision"
        } else {
            "$baseUrl/config"
        }
        
        val response = client.get(url) {
            if (apiKey.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
        }
        
        val restResponse = response.body<RestResponse>()
        return restResponse.toProviderSnapshot()
    }

    override suspend fun connect(): Flow<ProviderSnapshot> = flow {
        isConnectedState = true

        try {
            val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
            // Add apiKey to query parameter or header if needed
            val wsUrlWithAuth = if (apiKey.isNotBlank()) {
                "$wsUrl/ws/events?apiKey=$apiKey"
            } else {
                "$wsUrl/ws/events"
            }
            
            client.webSocket(wsUrlWithAuth) {
                webSocketSession = this

                // Send authentication and subscription message
                send(
                    Frame.Text(
                        json.encodeToString(
                            buildJsonObject {
                                put("type", "subscribe")
                                put("projectId", projectId)
                                // Also include apiKey in message for compatibility
                                if (apiKey.isNotBlank()) {
                                    put("apiKey", apiKey)
                                }
                            }
                        )
                    )
                )

                // Listen for messages
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            val message = json.parseToJsonElement(text).jsonObject
                            val messageType = message["type"]?.jsonPrimitive?.content

                            when (messageType) {
                                "connected" -> {
                                    // Connection established
                                }

                                "subscribed" -> {
                                    // Successfully subscribed
                                }

                                "flag_created", "flag_updated", "flag_deleted",
                                "experiment_created", "experiment_updated", "experiment_deleted" -> {
                                    // Flag/experiment changed, fetch new snapshot
                                    // Fetch directly with apiKey header
                                    val url = "$baseUrl/config"
                                    val response = client.get(url) {
                                        if (apiKey.isNotBlank()) {
                                            header(HttpHeaders.Authorization, "Bearer $apiKey")
                                        }
                                    }
                                    val restResponse = response.body<RestResponse>()
                                    emit(restResponse.toProviderSnapshot())
                                }

                                "error" -> {
                                    val errorMsg = message["message"]?.jsonPrimitive?.content
                                    throw Exception("WebSocket error: $errorMsg")
                                }
                            }
                        } catch (e: Exception) {
                            // Invalid message - skip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            isConnectedState = false
            throw e
        } finally {
            isConnectedState = false
            webSocketSession = null
        }
    }

    override suspend fun disconnect() {
        webSocketSession?.close()
        webSocketSession = null
        isConnectedState = false
    }

    override fun isConnected(): Boolean {
        return isConnectedState && (webSocketSession?.isActive == true)
    }
}

