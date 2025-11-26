package io.maxluxs.flagship.core.util

import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.FlagsCache
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.provider.FlagsProvider

/**
 * Builder utility for creating FlagsConfig consistently across different frameworks.
 * 
 * Used by Spring Boot and Ktor plugins to ensure consistent initialization.
 */
object FlagsConfigBuilder {
    /**
     * Build FlagsConfig with common defaults.
     * 
     * @param appKey Application key
     * @param environment Environment name
     * @param providers List of providers
     * @param cache Optional cache (defaults to InMemoryCache)
     * @param logger Optional logger (defaults to DefaultLogger)
     * @return Configured FlagsConfig
     */
    fun build(
        appKey: String,
        environment: String,
        providers: List<FlagsProvider>,
        cache: FlagsCache = InMemoryCache(),
        logger: FlagsLogger = DefaultLogger()
    ): FlagsConfig {
        return FlagsConfig(
            appKey = appKey,
            environment = environment,
            providers = providers,
            cache = cache,
            logger = logger
        )
    }
    
    /**
     * Initialize Flagship if not already configured and return manager.
     * 
     * This is a convenience method used by framework integrations.
     * 
     * @param config FlagsConfig to use
     * @return FlagsManager instance
     */
    fun initializeIfNeeded(config: FlagsConfig): FlagsManager {
        if (!Flagship.isConfigured()) {
            Flagship.configure(config)
        }
        return Flagship.manager()
    }
}

