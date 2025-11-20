package io.maxluxs.flagship.provider.launchdarkly

/**
 * iOS implementation of LaunchDarkly adapter.
 * 
 * TODO: Implement using LaunchDarkly iOS SDK via cinterop or cocoapods.
 * For now, use REST provider as fallback on iOS.
 */
class IOSLaunchDarklyAdapter : LaunchDarklyAdapter {
    
    override suspend fun initialize() {
        throw NotImplementedError("LaunchDarkly iOS adapter not yet implemented. Use REST provider instead.")
    }

    override suspend fun refresh() {
        throw NotImplementedError("LaunchDarkly iOS adapter not yet implemented. Use REST provider instead.")
    }

    override fun getAllFlags(): Map<String, Any?> {
        throw NotImplementedError("LaunchDarkly iOS adapter not yet implemented. Use REST provider instead.")
    }

    override fun getRevision(): String? {
        return null
    }
}

