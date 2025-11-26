package io.maxluxs.flagship.sample

actual object ProviderPreferences {
    private var selectedProvider: ProviderType? = null
    
    actual fun saveSelectedProvider(type: ProviderType) {
        selectedProvider = type
    }
    
    actual fun getSelectedProvider(): ProviderType {
        return selectedProvider ?: ProviderType.MOCK
    }
    
    actual fun clearProvider() {
        selectedProvider = null
    }
}

