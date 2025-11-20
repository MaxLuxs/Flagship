package io.maxluxs.flagship.core.util

interface FlagsLogger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

object NoopLogger : FlagsLogger {
    override fun debug(tag: String, message: String) {}
    override fun info(tag: String, message: String) {}
    override fun warn(tag: String, message: String) {}
    override fun error(tag: String, message: String, throwable: Throwable?) {}
}

class DefaultLogger : FlagsLogger {
    override fun debug(tag: String, message: String) {
        println("DEBUG [$tag] $message")
    }

    override fun info(tag: String, message: String) {
        println("INFO [$tag] $message")
    }

    override fun warn(tag: String, message: String) {
        println("WARN [$tag] $message")
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        println("ERROR [$tag] $message")
        throwable?.printStackTrace()
    }
}

