package io.maxluxs.flagship.core.analytics

import io.maxluxs.flagship.core.model.ExperimentKey
import io.maxluxs.flagship.core.model.Variant
import io.maxluxs.flagship.core.util.currentTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tracks conversion metrics for experiments.
 * 
 * Allows tracking conversion events and calculating statistics for A/B tests.
 */
class ConversionMetrics {
    private data class ConversionData(
        var exposures: Int = 0,
        var conversions: Int = 0,
        var lastConversionTime: Long? = null
    )
    
    private val metrics = mutableMapOf<String, MutableMap<String, ConversionData>>()
    private val mutex = Mutex()
    
    /**
     * Record an exposure event (user saw the experiment variant).
     */
    suspend fun recordExposure(experimentKey: ExperimentKey, variant: String) {
        mutex.withLock {
            val experimentMetrics = metrics.getOrPut(experimentKey) { mutableMapOf() }
            val variantMetrics = experimentMetrics.getOrPut(variant) { ConversionData() }
            variantMetrics.exposures++
        }
    }
    
    /**
     * Record a conversion event (user completed the desired action).
     */
    suspend fun recordConversion(experimentKey: ExperimentKey, variant: String) {
        mutex.withLock {
            val experimentMetrics = metrics.getOrPut(experimentKey) { mutableMapOf() }
            val variantMetrics = experimentMetrics.getOrPut(variant) { ConversionData() }
            variantMetrics.conversions++
            variantMetrics.lastConversionTime = currentTimeMillis()
        }
    }
    
    /**
     * Get conversion rate for a variant.
     */
    suspend fun getConversionRate(experimentKey: ExperimentKey, variant: String): Double? {
        return mutex.withLock {
            val variantMetrics = metrics[experimentKey]?.get(variant) ?: return null
            if (variantMetrics.exposures == 0) return null
            variantMetrics.conversions.toDouble() / variantMetrics.exposures
        }
    }
    
    /**
     * Get all metrics for an experiment.
     */
    suspend fun getExperimentMetrics(experimentKey: ExperimentKey): Map<String, VariantMetrics>? {
        return mutex.withLock {
            val experimentMetrics = metrics[experimentKey] ?: return null
            experimentMetrics.mapValues { (variant, data) ->
                VariantMetrics(
                    variant = variant,
                    exposures = data.exposures,
                    conversions = data.conversions,
                    conversionRate = if (data.exposures > 0) {
                        data.conversions.toDouble() / data.exposures
                    } else null,
                    lastConversionTime = data.lastConversionTime
                )
            }
        }
    }
    
    /**
     * Calculate statistical significance for an experiment.
     */
    suspend fun calculateSignificance(
        experimentKey: ExperimentKey,
        controlVariant: String = "control",
        confidenceLevel: Double = 0.95
    ): StatisticalResult? {
        return mutex.withLock {
            val experimentMetrics = metrics[experimentKey] ?: return null
            val control = experimentMetrics[controlVariant] ?: return null
            
            // Find best variant (highest conversion rate)
            val bestVariant = experimentMetrics.maxByOrNull { (_, data) ->
                if (data.exposures > 0) data.conversions.toDouble() / data.exposures else 0.0
            } ?: return null
            
            if (bestVariant.key == controlVariant) {
                return StatisticalResult(
                    isSignificant = false,
                    winner = null,
                    lift = 0.0,
                    pValue = 1.0
                )
            }
            
            val controlRate = if (control.exposures > 0) {
                control.conversions.toDouble() / control.exposures
            } else return null
            
            val variantRate = if (bestVariant.value.exposures > 0) {
                bestVariant.value.conversions.toDouble() / bestVariant.value.exposures
            } else return null
            
            val result = StatisticalAnalytics.calculateSignificance(
                controlConversions = control.conversions,
                controlTotal = control.exposures,
                variantConversions = bestVariant.value.conversions,
                variantTotal = bestVariant.value.exposures,
                confidenceLevel = confidenceLevel
            )
            
            return StatisticalResult(
                isSignificant = result.isStatisticallySignificant,
                winner = if (result.isStatisticallySignificant) bestVariant.key else null,
                lift = result.lift,
                pValue = result.pValue
            )
        }
    }
    
    /**
     * Reset metrics for an experiment.
     */
    suspend fun reset(experimentKey: ExperimentKey) {
        mutex.withLock {
            metrics.remove(experimentKey)
        }
    }
    
    /**
     * Reset all metrics.
     */
    suspend fun resetAll() {
        mutex.withLock {
            metrics.clear()
        }
    }
}

/**
 * Metrics for a specific variant.
 */
data class VariantMetrics(
    val variant: String,
    val exposures: Int,
    val conversions: Int,
    val conversionRate: Double?,
    val lastConversionTime: Long?
)

/**
 * Statistical result for experiment significance.
 */
data class StatisticalResult(
    val isSignificant: Boolean,
    val winner: String?,
    val lift: Double,
    val pValue: Double
)

