package io.maxluxs.flagship.core.evaluator

import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.TargetingRule
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TargetingEvaluatorTest {

    private val context = EvalContext(
        userId = "user1",
        deviceId = "device1",
        appVersion = "2.0.0",
        osName = "Android",
        osVersion = "12",
        region = "US",
        locale = "en-US",
        attributes = mapOf("tier" to "gold", "beta" to "true")
    )

    @Test
    fun `test AttributeEquals`() {
        val rule = TargetingRule.AttributeEquals("tier", "gold")
        assertTrue(TargetingEvaluator.evaluate(rule, context))

        val ruleFail = TargetingRule.AttributeEquals("tier", "silver")
        assertFalse(TargetingEvaluator.evaluate(ruleFail, context))
    }

    @Test
    fun `test RegionIn`() {
        val rule = TargetingRule.RegionIn(setOf("US", "CA"))
        assertTrue(TargetingEvaluator.evaluate(rule, context))

        val ruleFail = TargetingRule.RegionIn(setOf("UK", "DE"))
        assertFalse(TargetingEvaluator.evaluate(ruleFail, context))
    }

    @Test
    fun `test AppVersionGte`() {
        // 2.0.0 >= 1.5.0 -> True
        assertTrue(TargetingEvaluator.evaluate(TargetingRule.AppVersionGte("1.5.0"), context))
        
        // 2.0.0 >= 2.0.0 -> True
        assertTrue(TargetingEvaluator.evaluate(TargetingRule.AppVersionGte("2.0.0"), context))
        
        // 2.0.0 >= 2.1.0 -> False
        assertFalse(TargetingEvaluator.evaluate(TargetingRule.AppVersionGte("2.1.0"), context))
    }
    
    @Test
    fun `test SemVer parsing`() {
        // Custom verify of compareVersions logic via public API
        val ctxBeta = context.copy(appVersion = "1.0.0-beta")
        
        // 1.0.0-beta < 1.0.0
        // So if rule is Gte 1.0.0, then 1.0.0-beta should FAIL
        assertFalse(TargetingEvaluator.evaluate(TargetingRule.AppVersionGte("1.0.0"), ctxBeta))
        
        // 1.0.0 > 1.0.0-beta
        // So if rule is Gte 1.0.0-beta, then 1.0.0 should PASS
        assertTrue(TargetingEvaluator.evaluate(TargetingRule.AppVersionGte("1.0.0-beta"), context.copy(appVersion = "1.0.0")))
    }

    @Test
    fun `test Composite`() {
        // ALL match
        val ruleAll = TargetingRule.Composite(
            all = listOf(
                TargetingRule.AttributeEquals("tier", "gold"),
                TargetingRule.RegionIn(setOf("US"))
            )
        )
        assertTrue(TargetingEvaluator.evaluate(ruleAll, context))

        // ANY match (one fail, one pass)
        val ruleAny = TargetingRule.Composite(
            any = listOf(
                TargetingRule.AttributeEquals("tier", "silver"), // fail
                TargetingRule.RegionIn(setOf("US")) // pass
            )
        )
        assertTrue(TargetingEvaluator.evaluate(ruleAny, context))
        
        // Mixed
        val ruleMixed = TargetingRule.Composite(
            all = listOf(TargetingRule.AttributeEquals("tier", "gold")),
            any = listOf(TargetingRule.RegionIn(setOf("US")))
        )
        assertTrue(TargetingEvaluator.evaluate(ruleMixed, context))
    }
}

