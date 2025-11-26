package io.maxluxs.flagship.core.analytics

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConversionMetricsTest {
    
    @Test
    fun testRecordExposureAndConversion() = runTest {
        val metrics = ConversionMetrics()
        
        metrics.recordExposure("exp1", "control")
        metrics.recordExposure("exp1", "control")
        metrics.recordConversion("exp1", "control")
        
        val rate = metrics.getConversionRate("exp1", "control")
        assertNotNull(rate)
        assertEquals(0.5, rate, 0.01)
    }
    
    @Test
    fun testGetExperimentMetrics() = runTest {
        val metrics = ConversionMetrics()
        
        metrics.recordExposure("exp1", "control")
        metrics.recordExposure("exp1", "control")
        metrics.recordConversion("exp1", "control")
        
        metrics.recordExposure("exp1", "variant")
        metrics.recordExposure("exp1", "variant")
        metrics.recordExposure("exp1", "variant")
        metrics.recordConversion("exp1", "variant")
        metrics.recordConversion("exp1", "variant")
        
        val experimentMetrics = metrics.getExperimentMetrics("exp1")
        assertNotNull(experimentMetrics)
        assertEquals(2, experimentMetrics.size)
        assertEquals(0.5, experimentMetrics["control"]?.conversionRate ?: 0.0, 0.01)
        assertEquals(0.67, experimentMetrics["variant"]?.conversionRate ?: 0.0, 0.01)
    }
    
    @Test
    fun testCalculateSignificance() = runTest {
        val metrics = ConversionMetrics()
        
        // Control: 10 conversions out of 100 exposures (10%)
        repeat(100) { metrics.recordExposure("exp1", "control") }
        repeat(10) { metrics.recordConversion("exp1", "control") }
        
        // Variant: 15 conversions out of 100 exposures (15%)
        repeat(100) { metrics.recordExposure("exp1", "variant") }
        repeat(15) { metrics.recordConversion("exp1", "variant") }
        
        val result = metrics.calculateSignificance("exp1", "control")
        assertNotNull(result)
        // With these numbers, should not be significant (need more data)
        // But the calculation should work
        assertNotNull(result.winner)
    }
    
    @Test
    fun testReset() = runTest {
        val metrics = ConversionMetrics()
        
        metrics.recordExposure("exp1", "control")
        metrics.recordConversion("exp1", "control")
        
        assertNotNull(metrics.getExperimentMetrics("exp1"))
        
        metrics.reset("exp1")
        
        assertNull(metrics.getExperimentMetrics("exp1"))
    }
}

