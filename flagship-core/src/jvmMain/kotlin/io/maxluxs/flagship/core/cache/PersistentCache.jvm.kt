package io.maxluxs.flagship.core.cache

import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.util.FlagsSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

actual class PersistentCache actual constructor(
    private val serializer: FlagsSerializer
) : FlagsCache {
    private val cacheDir: File by lazy {
        val userHome = System.getProperty("user.home")
        val cachePath = Paths.get(userHome, ".flagship", "cache")
        val dir = cachePath.toFile()
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private fun getCacheFile(providerName: String): File {
        return File(cacheDir, "flagship_cache_$providerName.json")
    }

    actual override suspend fun save(providerName: String, snapshot: ProviderSnapshot) {
        withContext(Dispatchers.IO) {
            val data = serializer.serialize(snapshot)
            val file = getCacheFile(providerName)
            Files.write(
                file.toPath(),
                data.toByteArray(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )
        }
    }

    actual override suspend fun load(providerName: String): ProviderSnapshot? {
        return withContext(Dispatchers.IO) {
            val file = getCacheFile(providerName)
            if (!file.exists()) {
                return@withContext null
            }
            val data = String(Files.readAllBytes(file.toPath()))
            serializer.deserialize(data)
        }
    }

    actual override suspend fun clear(providerName: String) {
        withContext(Dispatchers.IO) {
            val file = getCacheFile(providerName)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    actual override suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("flagship_cache_")) {
                    file.delete()
                }
            }
        }
    }
}

