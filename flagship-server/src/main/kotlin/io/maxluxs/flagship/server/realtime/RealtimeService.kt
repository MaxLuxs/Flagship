package io.maxluxs.flagship.server.realtime

import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class RealtimeEvent(
    val type: String,
    val projectId: String? = null,
    val entityType: String? = null,
    val entityId: String? = null,
    val data: Map<String, String>? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class RealtimeService {
    private val logger = LoggerFactory.getLogger(RealtimeService::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    // Map of projectId -> Set of WebSocket sessions
    private val projectSubscriptions =
        ConcurrentHashMap<UUID, MutableSet<DefaultWebSocketSession>>()
    private val sessionProjects = ConcurrentHashMap<DefaultWebSocketSession, MutableSet<UUID>>()
    private val mutex = Mutex()

    // Event flow for broadcasting
    private val _events = MutableSharedFlow<RealtimeEvent>(replay = 0, extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    suspend fun subscribe(session: DefaultWebSocketSession, projectId: UUID) {
        mutex.withLock {
            projectSubscriptions.getOrPut(projectId) { mutableSetOf() }.add(session)
            sessionProjects.getOrPut(session) { mutableSetOf() }.add(projectId)
            logger.info("Session subscribed to project $projectId. Total subscriptions: ${projectSubscriptions[projectId]?.size}")
        }
    }

    suspend fun unsubscribe(session: DefaultWebSocketSession, projectId: UUID) {
        mutex.withLock {
            projectSubscriptions[projectId]?.remove(session)
            sessionProjects[session]?.remove(projectId)
            if (projectSubscriptions[projectId]?.isEmpty() == true) {
                projectSubscriptions.remove(projectId)
            }
            if (sessionProjects[session]?.isEmpty() == true) {
                sessionProjects.remove(session)
            }
            logger.info("Session unsubscribed from project $projectId")
        }
    }

    suspend fun unsubscribeAll(session: DefaultWebSocketSession) {
        mutex.withLock {
            val projects = sessionProjects.remove(session) ?: return@withLock
            projects.forEach { projectId ->
                projectSubscriptions[projectId]?.remove(session)
                if (projectSubscriptions[projectId]?.isEmpty() == true) {
                    projectSubscriptions.remove(projectId)
                }
            }
            logger.info("Session unsubscribed from all projects")
        }
    }

    suspend fun broadcast(event: RealtimeEvent) {
        _events.emit(event)

        val projectId = event.projectId?.let { UUID.fromString(it) }
        if (projectId != null) {
            val sessions = mutex.withLock {
                projectSubscriptions[projectId]?.toSet() ?: emptySet()
            }

            val message = json.encodeToString(RealtimeEvent.serializer(), event)
            sessions.forEach { session ->
                try {
                    if (session.isActive) {
                        session.send(Frame.Text(message))
                    } else {
                        unsubscribeAll(session)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to send message to session", e)
                    unsubscribeAll(session)
                }
            }

            logger.debug(
                "Broadcasted event {} to {} sessions for project {}",
                event.type,
                sessions.size,
                projectId
            )
        }
    }

    fun getSubscriptionCount(projectId: UUID): Int {
        return projectSubscriptions[projectId]?.size ?: 0
    }

    fun getTotalSubscriptions(): Int {
        return sessionProjects.size
    }
}

