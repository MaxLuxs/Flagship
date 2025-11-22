package io.maxluxs.flagship.core.provider

import io.maxluxs.flagship.core.model.ProviderSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Provider interface for realtime flag updates via SSE/WebSocket.
 * 
 * This extends FlagsProvider with realtime capabilities, allowing
 * flag changes to be pushed to the client without polling.
 * 
 * Example:
 * ```kotlin
 * class SSEFlagsProvider(
 *     private val baseUrl: String,
 *     private val apiKey: String
 * ) : RealtimeFlagsProvider {
 *     override suspend fun connect(): Flow<ProviderSnapshot> {
 *         // Connect to SSE endpoint and emit snapshots
 *     }
 *     
 *     override suspend fun disconnect() {
 *         // Close connection
 *     }
 * }
 * ```
 */
interface RealtimeFlagsProvider : FlagsProvider {
    /**
     * Connect to realtime stream and start receiving updates.
     * 
     * Returns a Flow that emits new snapshots whenever flags change.
     * The flow should handle reconnection automatically with exponential backoff.
     * 
     * @return Flow of ProviderSnapshot updates
     */
    suspend fun connect(): Flow<ProviderSnapshot>
    
    /**
     * Disconnect from realtime stream.
     * 
     * Stops receiving updates and closes the connection.
     */
    suspend fun disconnect()
    
    /**
     * Check if currently connected to realtime stream.
     * 
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean
}

