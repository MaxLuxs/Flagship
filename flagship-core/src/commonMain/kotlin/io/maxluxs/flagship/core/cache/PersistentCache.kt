package io.maxluxs.flagship.core.cache

import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.util.FlagsSerializer

expect class PersistentCache(serializer: FlagsSerializer) : FlagsCache {
    override suspend fun save(providerName: String, snapshot: ProviderSnapshot)
    override suspend fun load(providerName: String): ProviderSnapshot?
    override suspend fun clear(providerName: String)
    override suspend fun clearAll()
}

