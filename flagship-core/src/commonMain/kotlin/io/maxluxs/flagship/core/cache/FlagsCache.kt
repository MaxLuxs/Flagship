package io.maxluxs.flagship.core.cache

import io.maxluxs.flagship.core.model.ProviderSnapshot

interface FlagsCache {
    suspend fun save(providerName: String, snapshot: ProviderSnapshot)

    suspend fun load(providerName: String): ProviderSnapshot?

    suspend fun clear(providerName: String)

    suspend fun clearAll()
}

