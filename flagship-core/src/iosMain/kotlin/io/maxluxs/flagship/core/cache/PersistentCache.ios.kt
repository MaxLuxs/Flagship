package io.maxluxs.flagship.core.cache

import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.util.FlagsSerializer
import platform.Foundation.NSUserDefaults

actual class PersistentCache actual constructor(
    private val serializer: FlagsSerializer
) : FlagsCache {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual override suspend fun save(providerName: String, snapshot: ProviderSnapshot) {
        val data = serializer.serialize(snapshot)
        defaults.setObject(data, forKey = "flagship_cache_$providerName")
    }

    actual override suspend fun load(providerName: String): ProviderSnapshot? {
        val data = defaults.stringForKey("flagship_cache_$providerName") ?: return null
        return serializer.deserialize(data)
    }

    actual override suspend fun clear(providerName: String) {
        defaults.removeObjectForKey("flagship_cache_$providerName")
    }

    actual override suspend fun clearAll() {
        val keys = defaults.dictionaryRepresentation().keys
            .filterIsInstance<String>()
            .filter { it.startsWith("flagship_cache_") }
        
        keys.forEach { defaults.removeObjectForKey(it) }
    }
}

