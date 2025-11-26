package io.maxluxs.flagship.server.util

import io.maxluxs.flagship.shared.api.RestExperiment
import io.maxluxs.flagship.shared.api.RestFlagValue
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID
import java.util.regex.Pattern

data class ValidationResult(
    val isValid: Boolean,
    val error: String? = null
)

object ValidationUtils {
    private val EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )

    private val SLUG_PATTERN = Pattern.compile("^[a-z0-9-]+$")
    private val SLUG_REGEX = "^[a-z0-9]+(?:-[a-z0-9]+)*\$".toRegex()
    private val FLAG_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$")
    private val FLAG_KEY_REGEX = "^[a-zA-Z0-9_-]+$".toRegex()
    private val EXPERIMENT_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$")

    private const val MIN_EMAIL_LENGTH = 3
    private const val MAX_EMAIL_LENGTH = 255

    // Boolean versions for quick validation (used in AdminRoutes, ProjectRoutes)
    fun validateEmail(email: String): Boolean {
        if (email.isBlank() || email.length > 255) return false
        return EMAIL_PATTERN.matcher(email).matches()
    }

    // ValidationResult version for detailed error messages (used in AuthRoutes)
    fun validateEmailWithResult(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult(false, "Email cannot be empty")
        }

        if (email.length < MIN_EMAIL_LENGTH || email.length > MAX_EMAIL_LENGTH) {
            return ValidationResult(
                false,
                "Email must be between $MIN_EMAIL_LENGTH and $MAX_EMAIL_LENGTH characters"
            )
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult(false, "Invalid email format")
        }

        return ValidationResult(true)
    }

    fun validatePassword(password: String): ValidationResult {
        if (password.isBlank()) {
            return ValidationResult(false, "Password cannot be empty")
        }

        if (password.length < 8) {
            return ValidationResult(false, "Password must be at least 8 characters long")
        }
        if (password.length > 128) {
            return ValidationResult(false, "Password must be at most 128 characters long")
        }
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }

        if (!hasUpper || !hasLower || !hasDigit) {
            return ValidationResult(
                false,
                "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
            )
        }
        return ValidationResult(true)
    }

    // Boolean version for quick validation
    fun validateSlug(slug: String): Boolean {
        if (slug.isBlank() || slug.length > 100) return false
        if (slug.startsWith("-") || slug.endsWith("-")) return false
        return SLUG_PATTERN.matcher(slug).matches()
    }

    // ValidationResult version for detailed error messages
    fun validateSlugWithResult(slug: String): ValidationResult {
        if (slug.isBlank()) {
            return ValidationResult(false, "Slug cannot be empty")
        }

        if (slug.length < 1 || slug.length > 255) {
            return ValidationResult(false, "Slug must be between 1 and 255 characters")
        }

        if (!SLUG_REGEX.matches(slug)) {
            return ValidationResult(
                false,
                "Slug must contain only lowercase letters, numbers, and hyphens"
            )
        }

        return ValidationResult(true)
    }

    // Boolean version for quick validation
    fun validateFlagKey(key: String): Boolean {
        if (key.isBlank() || key.length > 100) return false
        return FLAG_KEY_PATTERN.matcher(key).matches()
    }

    // ValidationResult version for detailed error messages
    fun validateFlagKeyWithResult(key: String): ValidationResult {
        if (key.isBlank()) {
            return ValidationResult(false, "Flag key cannot be empty")
        }

        if (key.length < 1 || key.length > 255) {
            return ValidationResult(false, "Flag key must be between 1 and 255 characters")
        }

        if (!FLAG_KEY_REGEX.matches(key)) {
            return ValidationResult(
                false,
                "Flag key must contain only letters, numbers, underscores, and hyphens"
            )
        }

        return ValidationResult(true)
    }

    fun validateExperimentKey(key: String): Boolean {
        if (key.isBlank() || key.length > 100) return false
        return EXPERIMENT_KEY_PATTERN.matcher(key).matches()
    }

    fun validateUUID(uuidString: String): Boolean {
        return try {
            UUID.fromString(uuidString)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun validateRestFlagValue(flagValue: RestFlagValue): ValidationResult {
        val validTypes = setOf("bool", "int", "double", "string", "json", "number")
        if (!validTypes.contains(flagValue.type)) {
            return ValidationResult(
                false,
                "Invalid flag type: ${flagValue.type}. Must be one of: ${validTypes.joinToString()}"
            )
        }

        val value = flagValue.value
        when (flagValue.type) {
            "bool" -> {
                if (value !is JsonPrimitive || !value.isString) {
                    return ValidationResult(false, "Bool flag value must be a boolean")
                }
            }

            "int", "double", "number" -> {
                if (value !is JsonPrimitive) {
                    return ValidationResult(false, "Numeric flag value must be a number")
                }
            }

            "string" -> {
                if (value !is JsonPrimitive || !value.isString) {
                    return ValidationResult(false, "String flag value must be a string")
                }
            }

            "json" -> {
                // JSON can be any JsonElement, so just check it's not null
            }
        }

        return ValidationResult(true)
    }

    fun validateRestExperiment(experiment: RestExperiment): ValidationResult {
        if (experiment.variants.isEmpty()) {
            return ValidationResult(false, "Experiment must have at least one variant")
        }

        if (experiment.variants.size > 10) {
            return ValidationResult(false, "Experiment cannot have more than 10 variants")
        }

        val variantNames = mutableSetOf<String>()
        var totalWeight = 0.0

        for (variant in experiment.variants) {
            if (variant.name.isBlank() || variant.name.length > 50) {
                return ValidationResult(false, "Variant name must be between 1 and 50 characters")
            }

            if (variantNames.contains(variant.name)) {
                return ValidationResult(false, "Duplicate variant name: ${variant.name}")
            }
            variantNames.add(variant.name)

            if (variant.weight < 0.0 || variant.weight > 1.0) {
                return ValidationResult(false, "Variant weight must be between 0.0 and 1.0")
            }

            totalWeight += variant.weight
        }

        val weightTolerance = 0.001
        if (kotlin.math.abs(totalWeight - 1.0) > weightTolerance) {
            return ValidationResult(
                false,
                "Sum of variant weights must equal 1.0 (current: $totalWeight)"
            )
        }

        val validExposureTypes = setOf("onAssign", "onImpression", "onassign", "onimpression")
        if (!validExposureTypes.contains(experiment.exposureType.lowercase())) {
            return ValidationResult(false, "Invalid exposure type: ${experiment.exposureType}")
        }

        return ValidationResult(true)
    }
}


