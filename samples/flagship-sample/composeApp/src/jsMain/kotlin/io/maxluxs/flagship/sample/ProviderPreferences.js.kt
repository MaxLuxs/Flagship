package io.maxluxs.flagship.sample

actual object ProviderPreferences {
    private const val STORAGE_KEY = "flagship_provider_prefs"
    
    actual fun saveSelectedProvider(type: ProviderType) {
        kotlinx.browser.localStorage.setItem(STORAGE_KEY, type.name)
    }
    
    actual fun getSelectedProvider(): ProviderType {
        val saved = kotlinx.browser.localStorage.getItem(STORAGE_KEY)
        return try {
            ProviderType.valueOf(saved ?: ProviderType.MOCK.name)
        } catch (_: Exception) {
            ProviderType.MOCK
        }
    }
    
    actual fun clearProvider() {
        kotlinx.browser.localStorage.removeItem(STORAGE_KEY)
    }
}

