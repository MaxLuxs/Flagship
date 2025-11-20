package io.maxluxs.flagship.core.model

import kotlinx.serialization.Serializable

/**
 * Represents a user's assignment to an experiment variant.
 * 
 * This is the result of bucketing a user into a specific variant of an experiment.
 * Assignments are deterministic based on user ID and experiment key.
 * 
 * Example:
 * ```kotlin
 * val assignment = flags.assign("payment_flow_exp")
 * when (assignment?.variant) {
 *     "control" -> showLegacyFlow()
 *     "treatment" -> showNewFlow()
 * }
 * ```
 * 
 * @property key The experiment key this assignment belongs to
 * @property variant The assigned variant name (e.g., "control", "treatment", "A", "B")
 * @property payload Optional key-value data associated with this variant
 * @property hash Optional integrity hash for verification
 */
@Serializable
data class ExperimentAssignment(
    val key: ExperimentKey,
    val variant: String,
    val payload: Map<String, String> = emptyMap(),
    val hash: String? = null
)

