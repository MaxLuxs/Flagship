package io.maxluxs.flagship.core.provider

import io.maxluxs.flagship.core.model.ProviderSnapshot

/**
 * Marker interface for providers that support lazy loading.
 * 
 * Lazy providers are not loaded during bootstrap, but only when:
 * - A flag/experiment is requested that requires this provider
 * - An explicit load is requested
 * 
 * This is useful for:
 * - Secondary/fallback providers that are rarely used
 * - Providers with expensive initialization
 * - Providers that are only needed for specific features
 * 
 * Example:
 * ```kotlin
 * class ExpensiveProvider : FlagsProvider, LazyFlagsProvider {
 *     override suspend fun bootstrap(): ProviderSnapshot {
 *         // Expensive operation
 *         return fetchFromSlowSource()
 *     }
 * }
 * ```
 */
interface LazyFlagsProvider {
    /**
     * Whether this provider should be loaded lazily.
     * 
     * @return true if provider should be loaded on-demand, false if it should be loaded during bootstrap
     */
    val isLazy: Boolean
        get() = true
}

/**
 * Extension to check if a provider supports lazy loading.
 */
fun FlagsProvider.isLazyProvider(): Boolean {
    return this is LazyFlagsProvider && this.isLazy
}

