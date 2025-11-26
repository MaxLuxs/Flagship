package io.maxluxs.flagship.samplektor.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.model.ExperimentKey

/**
 * Routes for experiment-related endpoints.
 */
fun Routing.experimentsRoutes() {
    route("/api/experiments") {
        /**
         * Get experiment assignment.
         * GET /api/experiments/{key}
         */
        get("/{key}") {
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
    }
}

