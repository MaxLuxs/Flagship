package io.maxluxs.flagship.helpers

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.helpers.TestProvider
import io.maxluxs.flagship.sample.MockFlagsProvider

/**
 * Helper functions for creating test configurations
 */
object TestFlagsConfig {
    
    fun createTestConfig(
        providers: List<FlagsProvider> = listOf(MockFlagsProvider()),
        appKey: String = "test-app",
        environment: String = "test"
    ): FlagsConfig {
        return FlagsConfig(
            appKey = appKey,
            environment = environment,
            providers = providers,
            cache = InMemoryCache(),
            logger = DefaultLogger()
        )
    }
    
    fun createConfigWithMockFlags(
        flags: Map<String, io.maxluxs.flagship.core.model.FlagValue>,
        appKey: String = "test-app",
        environment: String = "test"
    ): FlagsConfig {
        return createTestConfig(
            providers = listOf(TestProvider(flags = flags)),
            appKey = appKey,
            environment = environment
        )
    }
}

