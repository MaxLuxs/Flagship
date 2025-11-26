package io.maxluxs.flagship.core.platform

import io.maxluxs.flagship.core.cache.PersistentCache
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.util.JsonSerializer
import kotlinx.browser.localStorage
import kotlinx.browser.window

object JsFlagsInitializer {
    /**
     * Create a PersistentCache for Web/JS
     */
    fun createPersistentCache(): PersistentCache {
        return PersistentCache(JsonSerializer())
    }

    /**
     * Create a default EvalContext for Web/JS
     */
    fun createDefaultContext(): EvalContext {
        val navigator = window.navigator
        val language = navigator.language ?: "en"
        val localeParts = language.split("-")
        val locale = localeParts[0]
        val region = if (localeParts.size > 1) localeParts[1] else ""
        
        return EvalContext(
            userId = null, // Should be set by app
            deviceId = getDeviceId(),
            appVersion = getAppVersion(),
            osName = navigator.platform ?: "Unknown",
            osVersion = navigator.userAgent ?: "Unknown",
            locale = language,
            region = region,
            attributes = emptyMap()
        )
    }

    private fun getDeviceId(): String {
        val storedId = localStorage.getItem("flagship_device_id")
        if (storedId != null) {
            return storedId
        }
        
        val deviceId = generateUUID()
        localStorage.setItem("flagship_device_id", deviceId)
        return deviceId
    }

    private fun getAppVersion(): String {
        val metaVersion = window.document.querySelector("meta[name='flagship-app-version']")
        return (metaVersion?.getAttribute("content") as? String) ?: "unknown"
    }

    private fun generateUUID(): String {
        return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(Regex("[xy]")) { matchResult ->
            val r = (js("Math.random()").unsafeCast<Double>() * 16.0).toInt() or 0
            val v = if (matchResult.value == "x") r else ((r and 0x3) or 0x8)
            v.toString(16)
        }
    }
}

