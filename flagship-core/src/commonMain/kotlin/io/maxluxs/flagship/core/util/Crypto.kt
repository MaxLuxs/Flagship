package io.maxluxs.flagship.core.util

interface Crypto {
    fun decrypt(data: String): String?
    fun verify(data: String, signature: String): Boolean
}

