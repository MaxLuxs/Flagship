package io.maxluxs.flagship.core.util

/**
 * Get current time in milliseconds since Unix epoch.
 */
expect fun currentTimeMillis(): Long

/**
 * Execute block with lock synchronization.
 * 
 * Note: For suspend functions, prefer using Mutex from kotlinx.coroutines.sync
 */
expect inline fun <T> synchronized(lock: Any, block: () -> T): T

