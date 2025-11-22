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
    val defaultValue: String? = null
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
        return json.decodeFromString(FlagsConfig.serializer(), content)
    }
}

