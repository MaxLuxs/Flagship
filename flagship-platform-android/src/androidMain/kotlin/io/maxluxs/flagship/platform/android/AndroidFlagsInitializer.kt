package io.maxluxs.flagship.platform.android

import android.content.Context
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.PersistentCache
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.util.JsonSerializer

object AndroidFlagsInitializer {
    /**
     * Create a PersistentCache for Android
     */
    fun createPersistentCache(context: Context): PersistentCache {
        return PersistentCache(JsonSerializer()).apply {
            initialize(context)
        }
    }

    /**
     * Create a default EvalContext for Android
     */
    fun createDefaultContext(context: Context): EvalContext {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        
        return EvalContext(
            userId = null, // Should be set by app
            deviceId = getDeviceId(context),
            appVersion = packageInfo.versionName ?: "unknown",
            osName = "Android",
            osVersion = android.os.Build.VERSION.RELEASE,
            locale = context.resources.configuration.locales[0].toString(),
            region = context.resources.configuration.locales[0].country,
            attributes = emptyMap()
        )
    }

    private fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences("flagship_device", Context.MODE_PRIVATE)
        var deviceId = prefs.getString("device_id", null)
        
        if (deviceId == null) {
            deviceId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", deviceId).apply()
        }
        
        return deviceId
    }
}

