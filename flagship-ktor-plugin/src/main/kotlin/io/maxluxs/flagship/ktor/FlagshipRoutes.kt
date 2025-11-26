package io.maxluxs.flagship.ktor

import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.manager.FlagsManager
import io.maxluxs.flagship.core.model.ExperimentKey
import io.maxluxs.flagship.core.model.FlagKey
import io.maxluxs.flagship.core.util.FlagValueUtils

/**
 * Adds Flagship REST endpoints to routing.
 */
fun Routing.flagshipRoutes(flagsManager: FlagsManager) {
    route("/api/flagship") {
        /**
         * Check if a flag is enabled.
         * GET /api/flagship/flags/{key}?default=false
         */
        get("/flags/{key}") {
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

        /**
         * Get a typed flag value.
         * GET /api/flagship/flags/{key}/value?default=0&type=int
         */
        get("/flags/{key}/value") {
            val key = call.parameters["key"] as FlagKey
            val default = call.request.queryParameters["default"] ?: ""
            val type = call.request.queryParameters["type"] ?: "string"

            val parsedDefault = FlagValueUtils.parseTypedValue(type, default)
            val value = Flagship.get(key, parsedDefault)

            call.respond(
                mapOf(
                    "key" to key,
                    "value" to value,
                    "type" to type
                )
            )
        }

        /**
         * Get experiment assignment.
         * GET /api/flagship/experiments/{key}
         */
        get("/experiments/{key}") {
            val key = call.parameters["key"] as ExperimentKey
            val assignment = Flagship.experiment(key)

            call.respond(
                mapOf(
                    "key" to key,
                    "variant" to assignment?.variant,
                    "payload" to assignment?.payload
                )
            )
        }

        /**
         * Get all flags.
         * GET /api/flagship/flags
         */
        get("/flags") {
            val allFlags = flagsManager.listAllFlags()
            val result = FlagValueUtils.flagsToJson(allFlags)

            call.respond(result)
        }
    }
}

