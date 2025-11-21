package io.maxluxs.flagship.core.platform

import io.maxluxs.flagship.core.cache.PersistentCache
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.util.JsonSerializer
import platform.Foundation.*
import platform.UIKit.UIDevice

object IOSFlagsInitializer {
    /**
     * Create a PersistentCache for iOS
     */
    fun createPersistentCache(): PersistentCache {
        return PersistentCache(JsonSerializer())
    }

    /**
     * Create a default EvalContext for iOS
     */
    fun createDefaultContext(): EvalContext {
        val device = UIDevice.currentDevice
        val locale = NSLocale.currentLocale
        
        return EvalContext(
            userId = null, // Should be set by app
            deviceId = getDeviceId(),
            appVersion = getAppVersion(),
            osName = "iOS",
            osVersion = device.systemVersion,
            locale = locale.localeIdentifier,
            region = locale.countryCode,
            attributes = emptyMap()
        )
    }

    private fun getDeviceId(): String {
        val defaults = NSUserDefaults.standardUserDefaults
        var deviceId = defaults.stringForKey("flagship_device_id")
        
        if (deviceId == null) {
            deviceId = NSUUID().UUIDString
            defaults.setObject(deviceId, forKey = "flagship_device_id")
        }
        
        return deviceId
    }

    private fun getAppVersion(): String {
        val bundle = NSBundle.mainBundle
        return bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
            ?: "unknown"
    }
}

