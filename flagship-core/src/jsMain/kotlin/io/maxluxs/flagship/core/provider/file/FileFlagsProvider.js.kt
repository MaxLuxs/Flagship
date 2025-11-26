package io.maxluxs.flagship.core.provider.file

/**
 * JS implementation of file reading.
 * 
 * Note: File reading is not supported in browser environment.
 * This provider should not be used in JS/web context.
 */
actual suspend fun readFileContent(provider: FileFlagsProvider, filePath: String): String? {
    // File reading is not supported in browser/JS environment
    return null
}

