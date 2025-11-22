package io.maxluxs.flagship.spring

import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.model.ExperimentKey
import io.maxluxs.flagship.core.model.FlagKey
import io.maxluxs.flagship.core.util.FlagValueUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for Flagship feature flags.
 *
 * Provides endpoints for checking flags and experiments.
 */
@RestController
@RequestMapping("/api/flagship")
class FlagshipController {

    /**
     * Check if a flag is enabled.
     *
     * GET /api/flagship/flags/{key}?default=false
     */
    @GetMapping("/flags/{key}")
    suspend fun getFlag(
        @PathVariable key: FlagKey,
        @RequestParam(defaultValue = "false") default: Boolean
    ): Map<String, Any> {
        val enabled = Flagship.isEnabled(key, default)
        return mapOf(
            "key" to key,
            "enabled" to enabled
        )
    }

    /**
     * Get a typed flag value.
     *
     * GET /api/flagship/flags/{key}/value?default=0&type=int
     */
    @GetMapping("/flags/{key}/value")
    suspend fun getFlagValue(
        @PathVariable key: FlagKey,
        @RequestParam default: String,
        @RequestParam type: String = "string"
    ): Map<String, Any> {
        val parsedDefault = FlagValueUtils.parseTypedValue(type, default)
        val value = Flagship.get(key, parsedDefault)

        return mapOf(
            "key" to key,
            "value" to value,
            "type" to type
        )
    }

    /**
     * Get experiment assignment.
     *
     * GET /api/flagship/experiments/{key}
     */
    @GetMapping("/experiments/{key}")
    suspend fun getExperiment(@PathVariable key: ExperimentKey): Map<String, Any?> {
        val assignment = Flagship.experiment(key)
        return mapOf(
            "key" to key,
            "variant" to assignment?.variant,
            "payload" to assignment?.payload
        )
    }

    /**
     * Get all flags.
     *
     * GET /api/flagship/flags
     */
    @GetMapping("/flags")
    suspend fun getAllFlags(): Map<String, Any?> {
        val manager = Flagship.manager()
        val allFlags = manager.listAllFlags()

        return FlagValueUtils.flagsToJson(allFlags)
    }
}

