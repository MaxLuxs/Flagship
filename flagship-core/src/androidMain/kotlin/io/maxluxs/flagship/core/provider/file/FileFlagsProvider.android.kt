package io.maxluxs.flagship.core.provider.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual suspend fun readFileContent(provider: FileFlagsProvider, filePath: String): String? {
    return withContext(Dispatchers.IO) {
        // Try as absolute path first
        val file = File(filePath)
        if (file.exists() && file.isFile) {
            return@withContext file.readText()
        }
        
        // Try as asset file (if context is available)
        // Note: This requires context injection, which is not implemented here
        // For full implementation, FileFlagsProvider would need to accept Context
        
        null
    }
}

