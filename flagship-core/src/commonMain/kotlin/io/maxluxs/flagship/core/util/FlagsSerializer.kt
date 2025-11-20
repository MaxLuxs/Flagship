package io.maxluxs.flagship.core.util

import io.maxluxs.flagship.core.model.ProviderSnapshot
import kotlinx.serialization.json.Json

interface FlagsSerializer {
    fun serialize(snapshot: ProviderSnapshot): String
    fun deserialize(data: String): ProviderSnapshot?
}

class JsonSerializer : FlagsSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override fun serialize(snapshot: ProviderSnapshot): String {
        return json.encodeToString(ProviderSnapshot.serializer(), snapshot)
    }

    override fun deserialize(data: String): ProviderSnapshot? {
        return try {
            json.decodeFromString(ProviderSnapshot.serializer(), data)
        } catch (e: Exception) {
            null
        }
    }
}

