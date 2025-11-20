package io.maxluxs.flagship.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ExperimentDefinition(
    val key: ExperimentKey,
    val variants: List<Variant>,
    val targeting: TargetingRule? = null,
    val exposureType: ExposureType = ExposureType.OnAssign
)

@Serializable
enum class ExposureType {
    OnAssign,
    OnImpression
}

