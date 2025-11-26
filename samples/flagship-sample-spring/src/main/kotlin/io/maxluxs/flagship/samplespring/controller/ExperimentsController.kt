package io.maxluxs.flagship.samplespring.controller

import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.model.EvalContext
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for experiments endpoints.
 */
@RestController
@RequestMapping("/api/experiments")
class ExperimentsController(
    private val flagsManager: FlagsManager
) {
    
    /**
     * Get experiment assignment for a user.
     * 
     * GET /api/experiments/{key}
     * 
     * Query parameters:
     * - userId: Optional user ID for consistent bucketing
     * - deviceId: Optional device ID
     * - appVersion: Application version (default: "1.0.0")
     * - osName: Operating system name (default: "Spring Boot")
     * - osVersion: Operating system version (default: "11")
     * - region: User region (optional)
     */
    @GetMapping("/{key}")
    fun getExperiment(
        @PathVariable key: String,
        @RequestParam(required = false) userId: String?,
        @RequestParam(required = false) deviceId: String?,
        @RequestParam(defaultValue = "1.0.0") appVersion: String,
        @RequestParam(defaultValue = "Spring Boot") osName: String,
        @RequestParam(defaultValue = "11") osVersion: String,
        @RequestParam(required = false) region: String?
    ): ResponseEntity<Map<String, Any?>> {
        return runBlocking {
            val context = EvalContext(
                userId = userId,
                deviceId = deviceId,
                appVersion = appVersion,
                osName = osName,
                osVersion = osVersion,
                region = region
            )
            
            val assignment = flagsManager.assign(key, ctx = context)
            
            if (assignment != null) {
                ResponseEntity.ok(mapOf(
                    "key" to assignment.key,
                    "variant" to assignment.variant,
                    "payload" to assignment.payload
                ))
            } else {
                ResponseEntity.ok(mapOf(
                    "key" to key,
                    "variant" to null,
                    "message" to "User does not qualify for this experiment"
                ))
            }
        }
    }
}

