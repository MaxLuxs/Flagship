package io.maxluxs.flagship.core.provider.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory

@OptIn(ExperimentalForeignApi::class)
actual suspend fun readFileContent(provider: FileFlagsProvider, filePath: String): String? {
    return withContext(Dispatchers.Default) {
        // Try as absolute path first
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(filePath)) {
            val content = NSString.stringWithContentsOfFile(filePath, encoding = platform.Foundation.NSUTF8StringEncoding, error = null)
            return@withContext content as? String
        }
        
        // Try to find in bundle resources
        val bundle = NSBundle.mainBundle
        val resourcePath = bundle.pathForResource(filePath, ofType = null)
        if (resourcePath != null) {
            val content = NSString.stringWithContentsOfFile(resourcePath, encoding = platform.Foundation.NSUTF8StringEncoding, error = null)
            return@withContext content as? String
        }
        
        // Try in documents directory
        val documentsPath = NSHomeDirectory() + "/Documents"
        val fullPath = "$documentsPath/$filePath"
        if (fileManager.fileExistsAtPath(fullPath)) {
            val content = NSString.stringWithContentsOfFile(fullPath, encoding = platform.Foundation.NSUTF8StringEncoding, error = null)
            return@withContext content as? String
        }
        
        null
    }
}

