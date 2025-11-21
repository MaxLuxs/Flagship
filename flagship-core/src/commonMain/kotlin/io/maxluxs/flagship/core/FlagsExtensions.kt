package io.maxluxs.flagship.core

import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.ExperimentAssignment
import io.maxluxs.flagship.core.model.ExperimentKey
import io.maxluxs.flagship.core.model.FlagKey
import io.maxluxs.flagship.core.model.FlagValue

/**
 * Simplified API extensions for common remote config use cases.
 * 
 * These extensions allow using Flagship with minimal boilerplate,
 * making it easier than using Firebase Remote Config directly.
 * 
 * Note: Platform-specific initialization functions are provided by provider modules:
 * - Flags.initFirebase() - in flagship-provider-firebase
 * - Flags.initRest() - in flagship-provider-rest
 */

/**
 * Direct access to check if a flag is enabled.
 * 
 * No need to call Flags.manager() first!
 * 
 * Example:
 * ```kotlin
 * if (Flags.isEnabled("dark_mode")) {
 *     enableDarkTheme()
 * }
 * ```
 * 
 * @param key Flag key
 * @param default Default value if flag not found (default: false)
 * @return true if flag is enabled
 */
fun Flags.isEnabled(
    key: FlagKey,
    default: Boolean = false
): Boolean {
    return manager().isEnabled(key, default)
}

/**
 * Direct access to get a typed flag value.
 *
 * Example:
 * ```kotlin
 * val timeout: Int = Flags.value("api_timeout", default = 30)
 * val message: String = Flags.value("welcome_msg", default = "Hello")
 * ```
 *
 * @param key Flag key
 * @param default Default value
 * @return Flag value or default
 */
fun <T> Flags.value(
    key: FlagKey,
    default: T
): T {
    return manager().value(key, default)
}

/**
 * Direct access to assign experiment variant.
 * 
 * Example:
 * ```kotlin
 * val variant = Flags.assign("checkout_exp")?.variant
 * when (variant) {
 *     "control" -> showLegacy()
 *     "treatment" -> showNew()
 * }
 * ```
 * 
 * @param key Experiment key
 * @return Experiment assignment or null
 */
fun Flags.assign(key: ExperimentKey): ExperimentAssignment? {
    return manager().assign(key)
}

/**
 * Direct access to assign experiment with context.
 * 
 * @param key Experiment key
 * @param context Evaluation context
 * @return Experiment assignment or null
 */
fun Flags.assign(
    key: ExperimentKey,
    context: EvalContext
): ExperimentAssignment? {
    return manager().assign(key, context)
}

/**
 * Direct access to refresh flags.
 * 
 * Example:
 * ```kotlin
 * lifecycleScope.launch {
 *     Flags.refresh()
 * }
 * ```
 */
fun Flags.refresh() {
    manager().refresh()
}

/**
 * Direct access to set override (debug only).
 * 
 * @param key Flag key
 * @param value Override value
 */
fun Flags.setOverride(key: FlagKey, value: FlagValue) {
    manager().setOverride(key, value)
}

/**
 * Direct access to clear override.
 * 
 * @param key Flag key
 */
fun Flags.clearOverride(key: FlagKey) {
    manager().clearOverride(key)
}

