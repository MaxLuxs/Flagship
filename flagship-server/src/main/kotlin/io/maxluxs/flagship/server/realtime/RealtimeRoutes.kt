package io.maxluxs.flagship.server.realtime

import io.ktor.http.ContentType
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.maxluxs.flagship.server.auth.ProjectAccessService
import io.maxluxs.flagship.server.auth.getUserId
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.util.UUID

private val logger = LoggerFactory.getLogger("RealtimeRoutes")

fun Routing.realtimeRoutes(realtimeService: RealtimeService, accessService: ProjectAccessService) {
    val json = Json { ignoreUnknownKeys = true }

    route("/ws") {
        authenticate("jwt-auth") {
            webSocket("/events") {
                val userId = call.getUserId()?.let { UUID.fromString(it) }
                if (userId == null) {
                    close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Authentication required"))
                    return@webSocket
                }

                logger.info("WebSocket connection established for user $userId")

                try {
                    // Send welcome message
                    send(Frame.Text("""{"type":"connected","message":"WebSocket connected"}"""))

                    // Handle incoming messages
                    val incomingJob = launch {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                try {
                                    val text = frame.readText()
                                    val message = json.parseToJsonElement(text).jsonObject
                                    val messageType = message["type"]?.jsonPrimitive?.content

                                    when (messageType) {
                                        "subscribe" -> {
                                            val projectIdStr =
                                                message["projectId"]?.jsonPrimitive?.content
                                            if (projectIdStr != null) {
                                                val projectId = UUID.fromString(projectIdStr)

                                                // Check access
                                                if (accessService.checkProjectAccess(
                                                        userId,
                                                        projectId
                                                    )
                                                ) {
                                                    realtimeService.subscribe(
                                                        this as DefaultWebSocketSession,
                                                        projectId
                                                    )
                                                    send(Frame.Text("""{"type":"subscribed","projectId":"$projectIdStr"}"""))
                                                } else {
                                                    send(Frame.Text("""{"type":"error","message":"Access denied to project $projectIdStr"}"""))
                                                }
                                            }
                                        }

                                        "unsubscribe" -> {
                                            val projectIdStr =
                                                message["projectId"]?.jsonPrimitive?.content
                                            if (projectIdStr != null) {
                                                val projectId = UUID.fromString(projectIdStr)
                                                realtimeService.unsubscribe(
                                                    this as DefaultWebSocketSession,
                                                    projectId
                                                )
                                                send(Frame.Text("""{"type":"unsubscribed","projectId":"$projectIdStr"}"""))
                                            }
                                        }

                                        "ping" -> {
                                            send(Frame.Text("""{"type":"pong"}"""))
                                        }
                                    }
                                } catch (e: Exception) {
                                    logger.warn("Error processing WebSocket message", e)
                                    send(Frame.Text("""{"type":"error","message":"Invalid message format"}"""))
                                }
                            }
                        }
                    }

                    // Keep connection alive and handle disconnection
                    incomingJob.join()
                } catch (e: Exception) {
                    logger.error("WebSocket error", e)
                } finally {
                    realtimeService.unsubscribeAll(this as DefaultWebSocketSession)
                    logger.info("WebSocket connection closed for user $userId")
                }
            }
        }
    }

    // SSE endpoint as alternative
    route("/sse") {
        authenticate("jwt-auth") {
            get("/events") {
                val userId = call.getUserId()?.let { UUID.fromString(it) }
                if (userId == null) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.Unauthorized,
                        "Authentication required"
                    )
                    return@get
                }

                call.response.headers.append("Content-Type", "text/event-stream")
                call.response.headers.append("Cache-Control", "no-cache")
                call.response.headers.append("Connection", "keep-alive")

                val projectIdStr = call.request.queryParameters["projectId"]
                val projectId = projectIdStr?.let { UUID.fromString(it) }

                if (projectId != null && !accessService.checkProjectAccess(userId, projectId)) {
                    call.respond(io.ktor.http.HttpStatusCode.Forbidden, "Access denied")
                    return@get
                }

                try {
                    call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                        realtimeService.events.collect { event ->
                            if (projectId == null || event.projectId == projectId.toString()) {
                                val json = Json { ignoreUnknownKeys = true }
                                val data = json.encodeToString(RealtimeEvent.serializer(), event)
                                write("data: $data\n\n")
                                flush()
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error("SSE error", e)
                }
            }
        }
    }
}

