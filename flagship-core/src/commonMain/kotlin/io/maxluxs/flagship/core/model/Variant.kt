package io.maxluxs.flagship.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Variant(
    val name: String,
    val weight: Double,
    val payload: Map<String, String> = emptyMap()
)

