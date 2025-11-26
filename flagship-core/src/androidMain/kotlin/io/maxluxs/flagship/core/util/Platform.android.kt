package io.maxluxs.flagship.core.util

actual fun getEnvironmentVariables(): Map<String, String> = System.getenv()

actual inline fun <T> synchronized(lock: Any, block: () -> T): T =
    kotlin.synchronized(lock, block)

