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
 * - Flagship.initFirebase() - in flagship-provider-firebase
 * - Flagship.initRest() - in flagship-provider-rest
 */

/**
 * Direct access to get a typed flag value.
 *
 * Example:
 * ```kotlin
 * lifecycleScope.launch {
 *     val timeout: Int = Flagship.value("api_timeout", default = 30)
 *     val message: String = Flagship.value("welcome_msg", default = "Hello")
 * }
 * ```
 *
 * @param key Flag key
 * @param default Default value
 * @return Flag value or default
 */
suspend fun <T> Flagship.value(
    key: FlagKey,
    default: T
): T {
    return manager().value(key, default)
}

