package io.maxluxs.flagship.core.model

import kotlinx.serialization.Serializable

@Serializable
data class PercentageGate(val percent: Int) {
    init {
        require(percent in 0..100) { "Percent must be between 0 and 100" }
    }
}

