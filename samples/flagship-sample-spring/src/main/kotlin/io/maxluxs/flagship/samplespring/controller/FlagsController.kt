package io.maxluxs.flagship.samplespring.controller

import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.FlagValue
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for feature flags endpoints.
 */
@RestController
@RequestMapping("/api/flags")
class FlagsController(
    private val flagsManager: FlagsManager
) {
    
    /**
     * List all flags.
     * 
     * GET /api/flags
     */
    @GetMapping
    fun listFlags(): ResponseEntity<Map<String, Any>> {
        return runBlocking {
            val flags = flagsManager.listAllFlags()
            val result = flags.mapValues { (_, value) ->
                when (value) {
                    is FlagValue.Bool -> mapOf("type" to "bool", "value" to value.value)
                    is FlagValue.Int -> mapOf("type" to "int", "value" to value.value)
                    is FlagValue.Double -> mapOf("type" to "double", "value" to value.value)
                    is FlagValue.StringV -> mapOf("type" to "string", "value" to value.value)
                    else -> mapOf("type" to "unknown", "value" to value.toString())
                }
            }
            ResponseEntity.ok(result)
        }
    }
    
    /**
     * Get flag value by key.
     * 
     * GET /api/flags/{key}
     */
    @GetMapping("/{key}")
    fun getFlag(
        @PathVariable key: String,
        @RequestParam(defaultValue = "false") default: String
    ): ResponseEntity<Map<String, Any>> {
        return runBlocking {
            val flagValue = flagsManager.listAllFlags()[key]
            if (flagValue != null) {
                val result = when (flagValue) {
                    is FlagValue.Bool -> mapOf("key" to key, "type" to "bool", "value" to flagValue.value)
                    is FlagValue.Int -> mapOf("key" to key, "type" to "int", "value" to flagValue.value)
                    is FlagValue.Double -> mapOf("key" to key, "type" to "double", "value" to flagValue.value)
                    is FlagValue.StringV -> mapOf("key" to key, "type" to "string", "value" to flagValue.value)
                    else -> mapOf("key" to key, "type" to "unknown", "value" to flagValue.toString())
                }
                ResponseEntity.ok(result)
            } else {
                ResponseEntity.notFound().build()
            }
        }
    }
    
    /**
     * Check if flag is enabled (boolean flag).
     * 
     * GET /api/flags/{key}/enabled
     */
    @GetMapping("/{key}/enabled")
    fun isEnabled(
        @PathVariable key: String,
        @RequestParam(defaultValue = "false") default: Boolean
    ): ResponseEntity<Map<String, Any>> {
        return runBlocking {
            val enabled = flagsManager.isEnabled(key, default = default)
            ResponseEntity.ok(mapOf(
                "key" to key,
                "enabled" to enabled
            ))
        }
    }
}

