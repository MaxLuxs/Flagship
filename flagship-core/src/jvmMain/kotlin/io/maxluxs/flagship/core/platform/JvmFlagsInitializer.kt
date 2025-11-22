package io.maxluxs.flagship.core.platform

import io.maxluxs.flagship.core.cache.PersistentCache
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.util.JsonSerializer
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object JvmFlagsInitializer {
    /**
     * Create a PersistentCache for JVM Desktop
     */
    fun createPersistentCache(): PersistentCache {
        return PersistentCache(JsonSerializer())
    }

    /**
     * Create a default EvalContext for JVM Desktop
     */
    fun createDefaultContext(): EvalContext {
        val properties = System.getProperties()
        val osName = properties.getProperty("os.name", "Unknown")
        val osVersion = properties.getProperty("os.version", "Unknown")
        val userHome = properties.getProperty("user.home", "")
        val locale = Locale.getDefault()
        
        return EvalContext(
            userId = null, // Should be set by app
            deviceId = getDeviceId(),
            appVersion = getAppVersion(),
            osName = osName,
            osVersion = osVersion,
            locale = locale.toString(),
            region = locale.country,
            attributes = emptyMap()
        )
    }

    private fun getDeviceId(): String {
        val userHome = System.getProperty("user.home")
        val deviceIdFile = Paths.get(userHome, ".flagship", "device_id.txt").toFile()
        
        if (deviceIdFile.exists()) {
            return deviceIdFile.readText().trim()
        }
        
        val deviceId = UUID.randomUUID().toString()
        deviceIdFile.parentFile?.mkdirs()
        deviceIdFile.writeText(deviceId)
        
        return deviceId
    }

    private fun getAppVersion(): String {
        val properties = System.getProperties()
        return properties.getProperty("flagship.app.version", "unknown")
    }
}

