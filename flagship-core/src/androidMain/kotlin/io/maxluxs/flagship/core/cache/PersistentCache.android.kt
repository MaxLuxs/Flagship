package io.maxluxs.flagship.core.cache

import android.content.Context
import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.util.FlagsSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class PersistentCache actual constructor(
    private val serializer: FlagsSerializer
) : FlagsCache {
    private var context: Context? = null

    fun initialize(context: Context) {
        this.context = context
    }

    private fun getPrefs() = context?.getSharedPreferences("flagship_cache", Context.MODE_PRIVATE)
        ?: throw IllegalStateException("PersistentCache not initialized. Call initialize(context) first.")

    actual override suspend fun save(providerName: String, snapshot: ProviderSnapshot) {
        withContext(Dispatchers.IO) {
            val data = serializer.serialize(snapshot)
            getPrefs().edit().putString(providerName, data).apply()
        }
    }

    actual override suspend fun load(providerName: String): ProviderSnapshot? {
        return withContext(Dispatchers.IO) {
            val data = getPrefs().getString(providerName, null) ?: return@withContext null
            serializer.deserialize(data)
        }
    }

    actual override suspend fun clear(providerName: String) {
        withContext(Dispatchers.IO) {
            getPrefs().edit().remove(providerName).apply()
        }
    }

    actual override suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            getPrefs().edit().clear().apply()
        }
    }
}

