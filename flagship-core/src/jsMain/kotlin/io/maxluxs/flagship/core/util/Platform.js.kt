package io.maxluxs.flagship.core.util

actual fun getEnvironmentVariables(): Map<String, String> {
    // JS doesn't have direct access to environment variables
    // Return empty map - can be extended with platform-specific implementation
    return emptyMap()
}

actual inline fun <T> synchronized(lock: Any, block: () -> T): T {
    // JS is single-threaded, so synchronization is not needed
    // Just execute the block directly
    return block()
}

