package io.maxluxs.flagship.core.realtime

import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.provider.RealtimeFlagsProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

/**
 * Manages realtime connections for flag providers.
 * 
 * Handles connection lifecycle, automatic reconnection with exponential backoff,
 * and snapshot updates to the FlagsManager.
 * 
 * Example:
 * ```kotlin
 * val realtimeManager = RealtimeManager(flagsManager, coroutineScope)
 * realtimeManager.connect(realtimeProvider)
 * ```
 */
class RealtimeManager(
    private val flagsManager: FlagsManager,
    private val scope: CoroutineScope,
    private val coroutineContext: CoroutineContext = Dispatchers.Default
) {
    private val connections = mutableMapOf<String, Job>()
    private val reconnectDelays = mutableMapOf<String, Long>()
    
    private val initialDelayMs = 1000L
    private val maxDelayMs = 60000L // 1 minute
    private val backoffMultiplier = 2.0
    
    private val backoffCalculator = io.maxluxs.flagship.core.util.BackoffCalculator
    
    /**
     * Connect to a realtime provider and start receiving updates.
     * 
     * Automatically handles reconnection with exponential backoff on failures.
     * Updates are automatically applied to the FlagsManager.
     * 
     * @param provider The realtime provider to connect to
     */
    fun connect(provider: RealtimeFlagsProvider) {
        val job = scope.launch(coroutineContext) {
            var currentDelay = initialDelayMs
            
            while (isActive) {
                try {
                    if (provider.isConnected()) {
                        currentDelay = initialDelayMs // Reset delay on successful connection
                    }
                    
                    provider.connect()
                        .catch { e ->
                            // Connection lost, schedule reconnect
                            reconnectDelays[provider.name] = currentDelay
                            delay(currentDelay)
                            currentDelay = backoffCalculator.nextDelay(currentDelay, maxDelayMs, backoffMultiplier)
                            throw e
                        }
                        .collect { snapshot ->
                            // Update flags manager with new snapshot
                            updateSnapshot(provider.name, snapshot)
                            currentDelay = initialDelayMs // Reset delay on successful update
                        }
                } catch (e: Exception) {
                    // Reconnect after delay
                    reconnectDelays[provider.name] = currentDelay
                    delay(currentDelay)
                    currentDelay = backoffCalculator.nextDelay(currentDelay, maxDelayMs, backoffMultiplier)
                }
            }
        }
        
        connections[provider.name] = job
    }
    
    /**
     * Disconnect from a realtime provider.
     * 
     * @param provider The realtime provider to disconnect from
     */
    suspend fun disconnect(provider: RealtimeFlagsProvider) {
        connections[provider.name]?.cancel()
        connections.remove(provider.name)
        reconnectDelays.remove(provider.name)
        provider.disconnect()
    }
    
    /**
     * Disconnect from all realtime providers.
     */
    suspend fun disconnectAll() {
        connections.values.forEach { it.cancel() }
        connections.clear()
        reconnectDelays.clear()
    }
    
    /**
     * Check if a provider is currently connected.
     * 
     * @param providerName The provider name
     * @return true if connected, false otherwise
     */
    fun isConnected(providerName: String): Boolean {
        return connections[providerName]?.isActive == true
    }
    
    private suspend fun updateSnapshot(providerName: String, snapshot: ProviderSnapshot) {
        if (flagsManager is DefaultFlagsManager) {
            flagsManager.updateSnapshotFromRealtime(providerName, snapshot)
        } else {
            // Fallback: trigger refresh
            flagsManager.refresh()
        }
    }
}

