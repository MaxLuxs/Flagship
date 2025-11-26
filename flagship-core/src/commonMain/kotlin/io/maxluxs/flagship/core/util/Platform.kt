package io.maxluxs.flagship.core.util

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Get current time in milliseconds since Unix epoch.
 */
@OptIn(ExperimentalTime::class)
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

/**
 * Get environment variables as a map.
 */
expect fun getEnvironmentVariables(): Map<String, String>

/**
 * Execute block with lock synchronization.
 * 
 * Note: For suspend functions, prefer using Mutex from kotlinx.coroutines.sync
 */
expect inline fun <T> synchronized(lock: Any, block: () -> T): T

