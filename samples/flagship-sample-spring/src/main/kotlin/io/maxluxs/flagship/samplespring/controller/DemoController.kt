package io.maxluxs.flagship.samplespring.controller

import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.model.EvalContext
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Demo endpoints showing real-world usage of Flagship.
 */
@RestController
@RequestMapping("/api/demo")
class DemoController(
    private val flagsManager: FlagsManager
) {
    
    /**
     * Demo endpoint using checkout_flow experiment.
     * 
     * GET /api/demo/checkout
     * 
     * Shows how to use experiment assignment to control checkout flow.
     */
    @GetMapping("/checkout")
    fun demoCheckout(
        @RequestParam(required = false) userId: String?
    ): ResponseEntity<Map<String, Any>> {
        return runBlocking {
            val context = EvalContext(
                userId = userId ?: "demo-user",
                appVersion = "1.0.0",
                osName = "Spring Boot",
                osVersion = "11"
            )
            
            val assignment = flagsManager.assign("checkout_flow", ctx = context)
            val variant = assignment?.variant ?: "control"
            
            val flow = when (variant) {
                "control" -> mapOf(
                    "flow" to "legacy",
                    "steps" to listOf("cart", "shipping", "payment", "review", "confirm"),
                    "features" to listOf("basic_validation")
                )
                "variant_a" -> mapOf(
                    "flow" to "streamlined",
                    "steps" to listOf("cart", "shipping_and_payment", "confirm"),
                    "features" to listOf("auto_fill", "quick_checkout")
                )
                "variant_b" -> mapOf(
                    "flow" to "enhanced",
                    "steps" to listOf("cart", "shipping", "payment", "review", "upsell", "confirm"),
                    "features" to listOf("recommendations", "extended_warranty")
                )
                else -> mapOf(
                    "flow" to "legacy",
                    "steps" to listOf("cart", "shipping", "payment", "review", "confirm"),
                    "features" to listOf("basic_validation")
                )
            }
            
            ResponseEntity.ok(mapOf(
                "experiment" to "checkout_flow",
                "variant" to variant,
                "checkoutFlow" to flow
            ))
        }
    }
    
    /**
     * Demo endpoint using new_feature flag.
     * 
     * GET /api/demo/feature
     * 
     * Shows how to use feature flags to enable/disable features.
     */
    @GetMapping("/feature")
    fun demoFeature(): ResponseEntity<Map<String, Any>> {
        return runBlocking {
            val newFeatureEnabled = flagsManager.isEnabled("new_feature", default = false)
            val paymentEnabled = flagsManager.isEnabled("payment_enabled", default = false)
            val maxRetries = flagsManager.value<Int>("max_retries", default = 3)
            val apiTimeout = flagsManager.value<Double>("api_timeout", default = 30.0)
            val welcomeMessage = flagsManager.value<String>("welcome_message", default = "Hello!")
            
            ResponseEntity.ok(mapOf(
                "newFeatureEnabled" to newFeatureEnabled,
                "paymentEnabled" to paymentEnabled,
                "maxRetries" to maxRetries,
                "apiTimeout" to apiTimeout,
                "welcomeMessage" to welcomeMessage,
                "features" to mapOf(
                    "newFeature" to if (newFeatureEnabled) "enabled" else "disabled",
                    "payment" to if (paymentEnabled) "enabled" else "disabled"
                )
            ))
        }
    }
}

