package io.maxluxs.flagship.core.evaluator

import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.ExperimentDefinition
import io.maxluxs.flagship.core.model.Variant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BucketingEngineTest {

    private val experiment = ExperimentDefinition(
        key = "test_exp",
        variants = listOf(
            Variant("A", 0.5),
            Variant("B", 0.5)
        )
    )

    @Test
    fun `test deterministic assignment`() {
        val context1 = EvalContext(userId = "user123", deviceId = "dev1", appVersion = "1.0", osName = "iOS", osVersion = "15.0", locale = "en", region = "US")
        val context2 = EvalContext(userId = "user123", deviceId = "dev2", appVersion = "1.0", osName = "iOS", osVersion = "15.0", locale = "en", region = "US")

        // Same user ID should get same variant
        val assignment1 = BucketingEngine.assign(experiment, context1)
        val assignment2 = BucketingEngine.assign(experiment, context2)

        assertEquals(assignment1?.variant, assignment2?.variant)
    }

    @Test
    fun `test distribution`() {
        val variants = mutableMapOf<String, Int>()
        val iterations = 1000

        for (i in 0 until iterations) {
            val ctx = EvalContext(userId = "user$i", deviceId = "dev", appVersion = "1.0", osName = "iOS", osVersion = "15.0", locale = "en", region = "US")
            val assignment = BucketingEngine.assign(experiment, ctx)
            val variant = assignment?.variant ?: "none"
            variants[variant] = variants.getOrElse(variant) { 0 } + 1
        }

        // Expect roughly 50/50 split
        val countA = variants["A"] ?: 0
        val countB = variants["B"] ?: 0
        
        // Allow 10% variance
        val variance = iterations * 0.1
        assertTrue(countA > (iterations / 2 - variance) && countA < (iterations / 2 + variance), "Distribution A ($countA) outside range")
        assertTrue(countB > (iterations / 2 - variance) && countB < (iterations / 2 + variance), "Distribution B ($countB) outside range")
    }
    
    @Test
    fun `test rollout percentage`() {
        val percent = 25
        var inBucketCount = 0
        val total = 1000
        
        for (i in 0 until total) {
            if (BucketingEngine.isInBucket("user$i", percent)) {
                inBucketCount++
            }
        }
        
        // Should be roughly 25%
        val expected = total * (percent / 100.0)
        val tolerance = total * 0.05 // 5% tolerance
        
        assertTrue(inBucketCount >= expected - tolerance && inBucketCount <= expected + tolerance, 
            "Bucket count $inBucketCount not within 5% tolerance of $expected")
    }
}
