package io.maxluxs.flagship.samplektor.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.model.FlagKey
import io.maxluxs.flagship.core.util.FlagValueUtils

/**
 * Routes for flag-related endpoints.
 */
fun Routing.flagsRoutes() {
    route("/api/flags") {
        /**
         * List all flags.
         * GET /api/flags
         */
        get {
            val manager = Flagship.manager()
            val allFlags = manager.listAllFlags()
            val result = FlagValueUtils.flagsToJson(allFlags)
            call.respond(result)
        }

        /**
         * Get flag value.
         * GET /api/flags/{key}?default=false
         */
        get("/{key}") {
            val key = call.parameters["key"] as FlagKey
            val defaultStr = call.request.queryParameters["default"] ?: ""
            
            // Try to infer type from default value
            val flagValue = when {
                defaultStr.isEmpty() -> Flagship.get(key, default = "")
                defaultStr.toBooleanStrictOrNull() != null -> Flagship.get(key, default = defaultStr.toBoolean())
                defaultStr.toIntOrNull() != null -> Flagship.get(key, default = defaultStr.toInt())
                defaultStr.toDoubleOrNull() != null -> Flagship.get(key, default = defaultStr.toDouble())
                else -> Flagship.get(key, default = defaultStr)
            }

            call.respond(
                mapOf(
                    "key" to key,
                    "value" to flagValue
                )
            )
        }

        /**
         * Check if flag is enabled.
         * GET /api/flags/{key}/enabled?default=false
         */
        get("/{key}/enabled") {
            val key = call.parameters["key"] as FlagKey
            val default = call.request.queryParameters["default"]?.toBoolean() ?: false
            val enabled = Flagship.isEnabled(key, default)

            call.respond(
                mapOf(
                    "key" to key,
                    "enabled" to enabled
                )
            )
        }
    }
}

