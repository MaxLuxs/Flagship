package io.maxluxs.flagship.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ProviderSnapshot(
    val flags: Map<FlagKey, FlagValue> = emptyMap(),
    val experiments: Map<ExperimentKey, ExperimentDefinition> = emptyMap(),
    val revision: String? = null,
    val fetchedAtMs: Long,
    val ttlMs: Long? = null
)

