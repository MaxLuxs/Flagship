package io.maxluxs.flagship

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform