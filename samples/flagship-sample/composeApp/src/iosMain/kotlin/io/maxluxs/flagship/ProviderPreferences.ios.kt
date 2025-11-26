package io.maxluxs.flagship.sample

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalForeignApi::class)
actual object ProviderPreferences {
    private const val KEY_PROVIDER = "selected_provider"
    
    actual fun saveSelectedProvider(type: ProviderType) {
        NSUserDefaults.standardUserDefaults.setObject(type.name, forKey = KEY_PROVIDER)
        NSUserDefaults.standardUserDefaults.synchronize()
    }
    
    actual fun getSelectedProvider(): ProviderType {
        val saved = NSUserDefaults.standardUserDefaults.stringForKey(KEY_PROVIDER)
        return try {
            ProviderType.valueOf(saved ?: ProviderType.MOCK.name)
        } catch (e: Exception) {
            ProviderType.MOCK
        }
    }
    
    actual fun clearProvider() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_PROVIDER)
        NSUserDefaults.standardUserDefaults.synchronize()
    }
}


