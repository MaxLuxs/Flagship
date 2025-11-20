package io.maxluxs.flagship.core.analytics

import kotlin.math.sqrt

/**
 * Statistical analytics for A/B testing.
 * Provides confidence intervals, statistical significance, and sample size calculations.
 *
 * Usage:
 * ```kotlin
 * val result = StatisticalAnalytics.calculateSignificance(
 *     controlConversions = 100,
 *     controlTotal = 1000,
 *     variantConversions = 120,
 *     variantTotal = 1000
 * )
 * 
 * if (result.isStatisticallySignificant) {
 *     println("Winner: ${result.winner} with ${result.lift}% lift")
 *     println("Confidence: ${result.confidenceLevel}%")
 * }
 * ```
 */
object StatisticalAnalytics {
    
    /**
     * Calculate statistical significance of A/B test results.
     *
     * @param controlConversions Number of conversions in control group
     * @param controlTotal Total number of users in control group
     * @param variantConversions Number of conversions in variant group
     * @param variantTotal Total number of users in variant group
     * @param confidenceLevel Desired confidence level (default: 95%)
     * @return Test result with significance, lift, and confidence interval
     */
    fun calculateSignificance(
        controlConversions: Int,
        controlTotal: Int,
        variantConversions: Int,
        variantTotal: Int,
        confidenceLevel: Double = 0.95
    ): TestResult {
        val controlRate = controlConversions.toDouble() / controlTotal
        val variantRate = variantConversions.toDouble() / variantTotal
        
        // Calculate lift
        val lift = ((variantRate - controlRate) / controlRate) * 100
        
        // Calculate standard error
        val se = sqrt(
            (controlRate * (1 - controlRate) / controlTotal) +
            (variantRate * (1 - variantRate) / variantTotal)
        )
        
        // Calculate z-score
        val zScore = (variantRate - controlRate) / se
        
        // Determine significance (using z-score threshold for 95% confidence: 1.96)
        val zThreshold = when (confidenceLevel) {
            0.90 -> 1.645
            0.95 -> 1.96
            0.99 -> 2.576
            else -> 1.96
        }
        
        val isSignificant = kotlin.math.abs(zScore) >= zThreshold
        val pValue = calculatePValue(zScore)
        
        // Calculate confidence interval for lift
        val marginOfError = zThreshold * se
        val lowerBound = ((controlRate - marginOfError - controlRate) / controlRate) * 100
        val upperBound = ((controlRate + marginOfError - controlRate) / controlRate) * 100
        
        return TestResult(
            controlRate = controlRate,
            variantRate = variantRate,
            lift = lift,
            confidenceInterval = Pair(lowerBound, upperBound),
            zScore = zScore,
            pValue = pValue,
            isStatisticallySignificant = isSignificant,
            confidenceLevel = confidenceLevel * 100,
            sampleSize = controlTotal + variantTotal,
            winner = when {
                !isSignificant -> "none"
                variantRate > controlRate -> "variant"
                else -> "control"
            }
        )
    }
    
    /**
     * Calculate required sample size for A/B test.
     *
     * @param baselineRate Expected baseline conversion rate (e.g., 0.05 for 5%)
     * @param minimumDetectableEffect Minimum detectable effect (e.g., 0.20 for 20% lift)
     * @param power Statistical power (default: 0.80 for 80%)
     * @param confidenceLevel Confidence level (default: 0.95 for 95%)
     * @return Required sample size per variant
     */
    fun calculateSampleSize(
        baselineRate: Double,
        minimumDetectableEffect: Double,
        power: Double = 0.80,
        confidenceLevel: Double = 0.95
    ): SampleSizeResult {
        val alpha = 1 - confidenceLevel
        val beta = 1 - power
        
        val zAlpha = when (confidenceLevel) {
            0.90 -> 1.645
            0.95 -> 1.96
            0.99 -> 2.576
            else -> 1.96
        }
        
        val zBeta = when (power) {
            0.80 -> 0.84
            0.90 -> 1.28
            0.95 -> 1.645
            else -> 0.84
        }
        
        val p1 = baselineRate
        val p2 = baselineRate * (1 + minimumDetectableEffect)
        val pBar = (p1 + p2) / 2
        
        val n = (
            (zAlpha * sqrt(2 * pBar * (1 - pBar)) + 
             zBeta * sqrt(p1 * (1 - p1) + p2 * (1 - p2))).let { it * it }
        ) / ((p2 - p1) * (p2 - p1))
        
        return SampleSizeResult(
            sampleSizePerVariant = kotlin.math.ceil(n).toInt(),
            totalSampleSize = kotlin.math.ceil(n * 2).toInt(),
            expectedDuration = calculateExpectedDuration(kotlin.math.ceil(n * 2).toInt()),
            baselineRate = baselineRate,
            minimumDetectableEffect = minimumDetectableEffect,
            power = power,
            confidenceLevel = confidenceLevel
        )
    }
    
    /**
     * Calculate confidence interval for conversion rate.
     */
    fun calculateConfidenceInterval(
        conversions: Int,
        total: Int,
        confidenceLevel: Double = 0.95
    ): Pair<Double, Double> {
        val rate = conversions.toDouble() / total
        val z = when (confidenceLevel) {
            0.90 -> 1.645
            0.95 -> 1.96
            0.99 -> 2.576
            else -> 1.96
        }
        
        val se = sqrt(rate * (1 - rate) / total)
        val marginOfError = z * se
        
        return Pair(
            maxOf(0.0, rate - marginOfError),
            minOf(1.0, rate + marginOfError)
        )
    }
    
    /**
     * Check if experiment has reached statistical significance.
     */
    fun hasReachedSignificance(
        controlConversions: Int,
        controlTotal: Int,
        variantConversions: Int,
        variantTotal: Int,
        confidenceLevel: Double = 0.95
    ): Boolean {
        return calculateSignificance(
            controlConversions, controlTotal,
            variantConversions, variantTotal,
            confidenceLevel
        ).isStatisticallySignificant
    }
    
    private fun calculatePValue(zScore: Double): Double {
        // Simplified p-value calculation
        // For production, use proper statistical library
        val absZ = kotlin.math.abs(zScore)
        return when {
            absZ >= 3.0 -> 0.001
            absZ >= 2.576 -> 0.01
            absZ >= 1.96 -> 0.05
            absZ >= 1.645 -> 0.10
            else -> 1.0
        }
    }
    
    private fun calculateExpectedDuration(totalSampleSize: Int): Int {
        // Assuming 1000 daily active users
        // Return expected days to reach sample size
        val dailyActiveUsers = 1000
        return (totalSampleSize.toDouble() / dailyActiveUsers).toInt()
    }
}

/**
 * Result of statistical significance test.
 */
data class TestResult(
    val controlRate: Double,
    val variantRate: Double,
    val lift: Double,
    val confidenceInterval: Pair<Double, Double>,
    val zScore: Double,
    val pValue: Double,
    val isStatisticallySignificant: Boolean,
    val confidenceLevel: Double,
    val sampleSize: Int,
    val winner: String
)

/**
 * Sample size calculation result.
 */
data class SampleSizeResult(
    val sampleSizePerVariant: Int,
    val totalSampleSize: Int,
    val expectedDuration: Int,
    val baselineRate: Double,
    val minimumDetectableEffect: Double,
    val power: Double,
    val confidenceLevel: Double
)

/**
 * Extension function to calculate experiment metrics.
 * Returns the statistical result for manual tracking.
 */
fun FlagsAnalytics.calculateExperimentMetrics(
    experimentKey: String,
    controlMetrics: Pair<Int, Int>, // (conversions, total)
    variantMetrics: Pair<Int, Int>, // (conversions, total)
    confidenceLevel: Double = 0.95
): TestResult {
    return StatisticalAnalytics.calculateSignificance(
        controlConversions = controlMetrics.first,
        controlTotal = controlMetrics.second,
        variantConversions = variantMetrics.first,
        variantTotal = variantMetrics.second,
        confidenceLevel = confidenceLevel
    )
}

