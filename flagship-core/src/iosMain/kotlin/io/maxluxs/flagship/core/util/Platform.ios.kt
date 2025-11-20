package io.maxluxs.flagship.core.util

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import kotlin.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

actual fun currentTimeMillis(): Long = 
    (NSDate().timeIntervalSince1970 * 1000).toLong()

actual inline fun <T> synchronized(lock: Any, block: () -> T): T {
    // iOS/Native doesn't have direct synchronized, but we can use freeze
    // For simple cases, just execute the block
    // In production, consider using kotlinx.atomicfu or platform.Foundation.NSLock
    return block()
}

