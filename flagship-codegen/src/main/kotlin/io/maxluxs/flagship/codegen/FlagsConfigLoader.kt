package io.maxluxs.flagship.codegen

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class FlagsConfig(
    val flags: List<FlagDefinition> = emptyList(),
    val experiments: List<ExperimentDefinition> = emptyList()
)

@Serializable
data class FlagDefinition(
    val key: String,
    val type: FlagType,
    val description: String? = null,
    val defaultValue: String? = null,
    val jsonType: String? = null, // Optional: fully qualified class name for JSON type (e.g., "com.example.MyData")
    val enumType: String? = null, // Optional: fully qualified enum class name (e.g., "com.example.PaymentMethod")
    val enumValues: List<String>? = null // Optional: list of enum values (required if enumType is specified)
)

@Serializable
data class ExperimentDefinition(
    val key: String,
    val description: String? = null,
    val variants: List<String> = emptyList()
)

@Serializable
enum class FlagType {
    BOOL,
    INT,
    DOUBLE,
    STRING,
    JSON
}

object FlagsConfigLoader {
    private val json = Json { ignoreUnknownKeys = true }
    
    fun load(file: File): FlagsConfig {
        if (!file.exists()) {
            throw IllegalArgumentException("Config file not found: ${file.absolutePath}")
        }
        
        val content = file.readText()
        val config = json.decodeFromString(FlagsConfig.serializer(), content)
        
        // Validate configuration
        validateConfig(config)
        
        return config
    }
    
    private fun validateConfig(config: FlagsConfig) {
        val errors = mutableListOf<String>()
        
        // Check for duplicate flag keys
        val flagKeys = config.flags.map { it.key }
        val duplicateFlags = flagKeys.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        if (duplicateFlags.isNotEmpty()) {
            errors.add("Duplicate flag keys: ${duplicateFlags.joinToString(", ")}")
        }
        
        // Check for duplicate experiment keys
        val experimentKeys = config.experiments.map { it.key }
        val duplicateExperiments = experimentKeys.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        if (duplicateExperiments.isNotEmpty()) {
            errors.add("Duplicate experiment keys: ${duplicateExperiments.joinToString(", ")}")
        }
        
        // Check for conflicts between flags and experiments
        val conflicts = flagKeys.intersect(experimentKeys.toSet())
        if (conflicts.isNotEmpty()) {
            errors.add("Key conflicts (used as both flag and experiment): ${conflicts.joinToString(", ")}")
        }
        
        // Validate flag definitions
        config.flags.forEachIndexed { index, flag ->
            // Validate flag key format (should be snake_case or kebab-case)
            if (!flag.key.matches(Regex("^[a-z][a-z0-9_-]*$"))) {
                errors.add("Flag at index $index: Invalid key format '${flag.key}'. Use snake_case or kebab-case (e.g., 'new_feature', 'api-timeout')")
            }
            
            // Validate default value matches type
            flag.defaultValue?.let { defaultValue ->
                try {
                    when (flag.type) {
                        FlagType.BOOL -> {
                            defaultValue.toBoolean() // Validate it's a boolean
                        }
                        FlagType.INT -> {
                            defaultValue.toInt() // Validate it's an int
                        }
                        FlagType.DOUBLE -> {
                            defaultValue.toDouble() // Validate it's a double
                        }
                        FlagType.STRING -> {
                            if (!defaultValue.startsWith("\"") || !defaultValue.endsWith("\"")) {
                                errors.add("Flag '${flag.key}': String defaultValue must be quoted (e.g., \"\\\"value\\\"\")")
                            }
                            Unit
                        }
                        FlagType.JSON -> {
                            // JSON should be valid JSON string
                            if (!defaultValue.startsWith("\"") || !defaultValue.endsWith("\"")) {
                                errors.add("Flag '${flag.key}': JSON defaultValue must be a quoted JSON string")
                            }
                            Unit
                        }
                    }
                } catch (e: Exception) {
                    errors.add("Flag '${flag.key}': Invalid defaultValue '${defaultValue}' for type ${flag.type}. ${e.message}")
                }
            }
            
            // Validate jsonType if provided
            flag.jsonType?.let { jsonType ->
                if (!jsonType.matches(Regex("^[a-zA-Z][a-zA-Z0-9_.]*$"))) {
                    errors.add("Flag '${flag.key}': Invalid jsonType format '${jsonType}'. Use fully qualified class name (e.g., 'com.example.MyData')")
                }
            }
            
            // Validate enumType if provided
            flag.enumType?.let { enumType ->
                if (!enumType.matches(Regex("^[a-zA-Z][a-zA-Z0-9_.]*$"))) {
                    errors.add("Flag '${flag.key}': Invalid enumType format '${enumType}'. Use fully qualified enum class name (e.g., 'com.example.PaymentMethod')")
                }
                if (flag.type != FlagType.STRING) {
                    errors.add("Flag '${flag.key}': enumType can only be used with STRING type")
                }
                if (flag.enumValues.isNullOrEmpty()) {
                    errors.add("Flag '${flag.key}': enumValues must be provided when enumType is specified")
                }
            }
            
            // Validate enumValues if provided
            flag.enumValues?.let { enumValues ->
                if (flag.enumType == null) {
                    errors.add("Flag '${flag.key}': enumType must be specified when enumValues is provided")
                }
                if (enumValues.isEmpty()) {
                    errors.add("Flag '${flag.key}': enumValues cannot be empty")
                }
                // Validate default value is in enum values
                flag.defaultValue?.let { defaultValue ->
                    val cleanDefault = defaultValue.trim('"')
                    if (!enumValues.contains(cleanDefault)) {
                        errors.add("Flag '${flag.key}': defaultValue '$cleanDefault' must be one of enumValues: ${enumValues.joinToString(", ")}")
                    }
                }
            }
        }
        
        // Validate experiment definitions
        config.experiments.forEachIndexed { index, experiment ->
            // Validate experiment key format
            if (!experiment.key.matches(Regex("^[a-z][a-z0-9_-]*$"))) {
                errors.add("Experiment at index $index: Invalid key format '${experiment.key}'. Use snake_case or kebab-case")
            }
            
            // Validate variants
            if (experiment.variants.isEmpty()) {
                errors.add("Experiment '${experiment.key}': Must have at least one variant")
            }
            
            // Check for duplicate variant names
            val variantNames = experiment.variants
            val duplicateVariants = variantNames.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
            if (duplicateVariants.isNotEmpty()) {
                errors.add("Experiment '${experiment.key}': Duplicate variant names: ${duplicateVariants.joinToString(", ")}")
            }
        }
        
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException("Configuration validation failed:\n${errors.joinToString("\n")}")
        }
    }
}

