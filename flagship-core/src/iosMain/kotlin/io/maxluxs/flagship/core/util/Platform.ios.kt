package io.maxluxs.flagship.core.util

actual fun getEnvironmentVariables(): Map<String, String> {
    // iOS doesn't have direct access to environment variables
    // Return empty map - can be extended with platform-specific implementation
    return emptyMap()
}

actual inline fun <T> synchronized(lock: Any, block: () -> T): T {
    // iOS/Native doesn't have direct synchronized, but we can use freeze
    // For simple cases, just execute the block
    // In production, consider using kotlinx.atomicfu or platform.Foundation.NSLock
    return block()
}

