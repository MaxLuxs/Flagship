package io.maxluxs.flagship.core.js

import io.maxluxs.flagship.core.evaluator.BucketingEngine
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.ExperimentDefinition
import io.maxluxs.flagship.core.model.Variant
import io.maxluxs.flagship.core.util.BackoffCalculator
import io.maxluxs.flagship.core.util.ExperimentParser
import kotlin.js.JsExport

/**
 * JavaScript/TypeScript exports for Flagship core functionality.
 * 
 * This module exports key functions that can be used from TypeScript/JavaScript,
 * allowing Node.js SDK to use shared Kotlin logic for:
 * - Deterministic bucketing (MurmurHash3)
 * - Experiment assignment
 * - Backoff calculations
 * 
 * Usage from TypeScript:
 * ```typescript
 * import { assignExperiment, calculateBackoff } from './kotlin/flagship-core.js';
 * 
 * const assignment = assignExperiment(experiment, context);
 * const delay = calculateBackoff(attempt, 1000, 60000);
 * ```
 */

@JsExport
object FlagshipJsExports {
    /**
     * Assign experiment variant using deterministic bucketing.
     * 
     * This uses the same MurmurHash3 algorithm as Kotlin implementation,
     * ensuring consistent assignments across platforms.
     * 
     * @param experimentKey Experiment key
     * @param userId User ID for bucketing
     * @param variants List of variants with weights
     * @return Assigned variant name, or null if no variants
     */
    @JsExport
    fun assignExperiment(
        experimentKey: String,
        userId: String,
        variants: Array<out VariantData>
    ): String? {
        if (variants.isEmpty()) {
            return null
        }
        
        val kotlinVariants = variants.map { 
            Variant(it.name, it.weight) 
        }
        
        val experiment = ExperimentDefinition(
            key = experimentKey,
            variants = kotlinVariants
        )
        
        val context = EvalContext(
            userId = userId,
            deviceId = userId,
            appVersion = null,
            osName = null,
            osVersion = null,
            locale = null,
            region = null,
            attributes = emptyMap()
        )
        
        val assignment = BucketingEngine.assign(experiment, context)
        return assignment?.variant
    }
    
    /**
     * Assign experiment with full context and targeting support.
     * 
     * @param experimentKey Experiment key
     * @param contextData User context data
     * @param variants List of variants with weights
     * @param targeting Optional targeting rules (JSON string)
     * @return Assigned variant name, or null if user doesn't qualify
     */
    @JsExport
    fun assignExperimentWithContext(
        experimentKey: String,
        contextData: ContextData,
        variants: Array<out VariantData>,
        targeting: String? = null
    ): ExperimentAssignmentResult? {
        if (variants.isEmpty()) {
            return null
        }
        
        val kotlinVariants = variants.map { 
            Variant(it.name, it.weight) 
        }
        
        val context = EvalContext(
            userId = contextData.userId,
            deviceId = contextData.deviceId ?: contextData.userId,
            appVersion = contextData.appVersion,
            osName = contextData.osName,
            osVersion = contextData.osVersion,
            locale = contextData.locale,
            region = contextData.region,
            attributes = contextData.attributes ?: emptyMap()
        )
        
        // Parse targeting rules from JSON if provided
        val targetingRule = targeting?.let { 
            ExperimentParser.parseTargetingFromJson(it) 
        }
        
        val experiment = ExperimentDefinition(
            key = experimentKey,
            variants = kotlinVariants,
            targeting = targetingRule
        )
        
        val assignment = BucketingEngine.assign(experiment, context)
        return assignment?.let {
            ExperimentAssignmentResult(
                variant = it.variant,
                hash = it.hash,
                payload = it.payload
            )
        }
    }
    
    /**
     * Calculate exponential backoff delay.
     * 
     * @param attempt Current attempt number (1-based)
     * @param initialDelayMs Initial delay in milliseconds
     * @param maxDelayMs Maximum delay cap
     * @param factor Exponential factor (default: 2.0)
     * @return Calculated delay in milliseconds
     */
    @JsExport
    fun calculateBackoff(
        attempt: Int,
        initialDelayMs: Long,
        maxDelayMs: Long,
        factor: Double = 2.0
    ): Long {
        return BackoffCalculator.calculateDelay(
            attempt = attempt,
            initialDelayMs = initialDelayMs,
            maxDelayMs = maxDelayMs,
            factor = factor
        )
    }
    
    /**
     * Calculate next backoff delay (for incremental backoff).
     * 
     * @param currentDelay Current delay in milliseconds
     * @param maxDelayMs Maximum delay cap
     * @param factor Multiplier (default: 2.0)
     * @return Next delay in milliseconds
     */
    @JsExport
    fun nextBackoff(
        currentDelay: Long,
        maxDelayMs: Long,
        factor: Double = 2.0
    ): Long {
        return BackoffCalculator.nextDelay(
            currentDelay = currentDelay,
            maxDelayMs = maxDelayMs,
            factor = factor
        )
    }
    
    /**
     * Check if user is in rollout bucket.
     * 
     * @param userId User ID
     * @param percent Rollout percentage (0-100)
     * @return true if user is in bucket
     */
    @JsExport
    fun isInBucket(userId: String, percent: Int): Boolean {
        return BucketingEngine.isInBucket(userId, percent)
    }
}

/**
 * Data class for variant information from JavaScript.
 */
@JsExport
data class VariantData(
    val name: String,
    val weight: Double
)

/**
 * User context data for experiment assignment.
 */
@JsExport
data class ContextData(
    val userId: String,
    val deviceId: String? = null,
    val appVersion: String? = null,
    val osName: String? = null,
    val osVersion: String? = null,
    val locale: String? = null,
    val region: String? = null,
    val attributes: Map<String, String>? = null
)

/**
 * Experiment assignment result.
 */
@JsExport
data class ExperimentAssignmentResult(
    val variant: String,
    val hash: String? = null,
    val payload: Map<String, String>? = null
)

