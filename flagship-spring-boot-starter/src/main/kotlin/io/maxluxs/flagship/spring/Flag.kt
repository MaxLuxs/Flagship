package io.maxluxs.flagship.spring

/**
 * Annotation for marking methods or fields that use feature flags.
 *
 * This is a marker annotation for documentation and tooling purposes.
 * To actually use flags, inject FlagsManager or use Flagship directly.
 *
 * Usage example:
 * ```kotlin
 * @Service
 * class MyService {
 *     @Flag("new_ui")
 *     fun shouldUseNewUI(): Boolean {
 *         return Flagship.isEnabled("new_ui", default = false)
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Flag(
    /**
     * The flag key.
     */
    val value: String,

    /**
     * Default value if flag is not found (as string).
     */
    val defaultValue: String = "false"
)

