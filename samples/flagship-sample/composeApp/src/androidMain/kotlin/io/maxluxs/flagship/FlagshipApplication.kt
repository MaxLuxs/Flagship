package io.maxluxs.flagship

import android.app.Application
import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.core.platform.AndroidFlagsInitializer
import io.maxluxs.flagship.sample.ProviderPreferences

class FlagshipApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ProviderPreferences.init(this)
        initializeFlagship()
    }

    private fun initializeFlagship() {
        val selectedProvider = ProviderPreferences.getSelectedProvider()
        val provider = ProviderFactory.createProvider(selectedProvider, this)
        
        val config = FlagsConfig(
            appKey = "sample-app",
            environment = "development",
            providers = listOf(provider),
            cache = AndroidFlagsInitializer.createPersistentCache(this)
        )

        Flagship.configure(config)
        
        // Set default context
        val manager = Flagship.manager() as DefaultFlagsManager
        val defaultContext = AndroidFlagsInitializer.createDefaultContext(this)
        manager.setDefaultContext(defaultContext)
    }
}

