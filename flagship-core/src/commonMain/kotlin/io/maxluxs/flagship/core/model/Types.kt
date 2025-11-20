package io.maxluxs.flagship.core.model

/**
 * Type alias for a feature flag key.
 * 
 * Convention: Use snake_case for flag names, e.g., "new_payment_flow", "dark_mode_enabled"
 */
typealias FlagKey = String

/**
 * Type alias for an experiment key.
 * 
 * Convention: Prefix with "exp_" for experiments, e.g., "exp_payment_flow", "exp_onboarding_variant"
 */
typealias ExperimentKey = String

