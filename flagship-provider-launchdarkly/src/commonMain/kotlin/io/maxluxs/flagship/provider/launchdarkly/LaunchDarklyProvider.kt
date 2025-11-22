package io.maxluxs.flagship.provider.launchdarkly

import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.BaseFlagsProvider
import io.maxluxs.flagship.core.util.ExperimentParser
import io.maxluxs.flagship.core.util.FlagValueParser
import io.maxluxs.flagship.core.util.SystemClock

/**
 * LaunchDarkly provider for Flagship.
 * Integrates with LaunchDarkly SDK for enterprise feature flag management.
 */
class LaunchDarklyProvider(
    private val adapter: LaunchDarklyAdapter,
    name: String = "launchdarkly"
) : BaseFlagsProvider(name) {
    
    override suspend fun fetchSnapshot(currentRevision: String?): ProviderSnapshot {
        adapter.initialize()
        return parseSnapshot()
    }
    
    override suspend fun refresh(): ProviderSnapshot {
        adapter.refresh()
        snapshot = parseSnapshot()
        return snapshot
    }

    private fun parseSnapshot(): ProviderSnapshot {
        val allFlags = adapter.getAllFlags()
        val flags = mutableMapOf<FlagKey, FlagValue>()
        val experiments = mutableMapOf<ExperimentKey, ExperimentDefinition>()

        allFlags.forEach { (key, value) ->
            when {
                key.startsWith("exp_") -> {
                    // Parse as experiment
                    parseExperiment(key, value)?.let { experiments[key] = it }
                }
                else -> {
                    // Parse as simple flag
                    parseFlag(value)?.let { flags[key] = it }
                }
            }
        }

        return ProviderSnapshot(
            flags = flags,
            experiments = experiments,
            revision = adapter.getRevision(),
            fetchedAtMs = SystemClock.currentTimeMillis(),
            ttlMs = 15 * 60_000 // 15 minutes default
        )
    }

    private fun parseFlag(value: Any?): FlagValue? {
        return FlagValueParser.parseFromAny(value)
    }

    private fun parseExperiment(key: String, value: Any?): ExperimentDefinition? {
        if (value !is String) return null
        return ExperimentParser.parseExperiment(key, value)
    }
}

/**
 * Adapter interface for LaunchDarkly SDK.
 * Platform-specific implementations wrap native LaunchDarkly SDKs.
 */
interface LaunchDarklyAdapter {
    suspend fun initialize()
    suspend fun refresh()
    fun getAllFlags(): Map<String, Any?>
    fun getRevision(): String?
}

