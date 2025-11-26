package io.maxluxs.flagship.samplektor.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.maxluxs.flagship.core.Flagship

/**
 * Demo routes showing real-world usage of Flagship.
 */
fun Routing.demoRoutes() {
    route("/api/demo") {
        /**
         * Demo endpoint using checkout_flow experiment.
         * GET /api/demo/checkout
         */
        get("/checkout") {
            val assignment = Flagship.experiment("checkout_flow")
            val variant = assignment?.variant ?: "control"

            val response = when (variant) {
                "control" -> mapOf(
                    "flow" to "legacy",
                    "variant" to variant,
                    "description" to "Traditional checkout flow with single-step payment"
                )
                "variant_a" -> mapOf(
                    "flow" to "new-a",
                    "variant" to variant,
                    "description" to "New checkout flow A with multi-step wizard"
                )
                "variant_b" -> mapOf(
                    "flow" to "new-b",
                    "variant" to variant,
                    "description" to "New checkout flow B with express checkout option"
                )
                else -> mapOf(
                    "flow" to "legacy",
                    "variant" to variant,
                    "description" to "Fallback to legacy checkout"
                )
            }

            call.respond(response)
        }

        /**
         * Demo endpoint using new_feature flag.
         * GET /api/demo/feature
         */
        get("/feature") {
            val newFeatureEnabled = Flagship.isEnabled("new_feature", default = false)
            val paymentEnabled = Flagship.isEnabled("payment_enabled", default = false)
            val maxRetries = Flagship.get("max_retries", default = 3)
            val apiTimeout = Flagship.get("api_timeout", default = 30.0)
            val welcomeMessage = Flagship.get("welcome_message", default = "Welcome!")

            call.respond(
                mapOf(
                    "new_feature_enabled" to newFeatureEnabled,
                    "payment_enabled" to paymentEnabled,
                    "max_retries" to maxRetries,
                    "api_timeout_seconds" to apiTimeout,
                    "welcome_message" to welcomeMessage,
                    "features" to buildList {
                        if (newFeatureEnabled) add("new_feature")
                        if (paymentEnabled) add("payment")
                    }
                )
            )
        }
    }
}

