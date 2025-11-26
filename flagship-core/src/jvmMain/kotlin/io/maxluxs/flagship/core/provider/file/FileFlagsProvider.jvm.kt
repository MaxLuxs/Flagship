package io.maxluxs.flagship.core.provider.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual suspend fun readFileContent(provider: FileFlagsProvider, filePath: String): String? {
    return withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            return@withContext null
        }
        file.readText()
    }
}

