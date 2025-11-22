package io.maxluxs.flagship.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.maxluxs.flagship.provider.rest.RestExperiment
import io.maxluxs.flagship.provider.rest.RestFlagValue

fun Routing.flagRoutes(storage: FlagStorage) {
    route("/api/flags") {
        // GET /api/flags - Get all flags
        get {
            val flags = storage.getAllFlags()
            call.respond(flags)
        }
        
        // GET /api/flags/{key} - Get specific flag
        get("{key}") {
            val key = call.parameters["key"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing flag key")
            )
            
            val flag = storage.getFlag(key)
            if (flag != null) {
                call.respond(mapOf(key to flag))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Flag not found"))
            }
        }
        
        // POST /api/flags - Create new flag
        post {
            try {
                val body = call.receive<Map<String, RestFlagValue>>()
                if (body.size != 1) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Expected exactly one flag key-value pair")
                    )
                    return@post
                }
                
                val (key, flag) = body.entries.first()
                val created = storage.createFlag(key, flag)
                call.respond(HttpStatusCode.Created, mapOf(key to created))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.Conflict, mapOf(("error" to e.message) ?: "Conflict"))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid request body: ${e.message}")
                )
            }
        }
        
        // PUT /api/flags/{key} - Update flag
        put("{key}") {
            val key = call.parameters["key"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing flag key")
            )
            
            try {
                val flag = call.receive<RestFlagValue>()
                val updated = storage.updateFlag(key, flag)
                if (updated != null) {
                    call.respond(mapOf(key to updated))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Flag not found"))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid request body: ${e.message}")
                )
            }
        }
        
        // DELETE /api/flags/{key} - Delete flag
        delete("{key}") {
            val key = call.parameters["key"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing flag key")
            )
            
            val deleted = storage.deleteFlag(key)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Flag not found"))
            }
        }
    }
}

fun Routing.experimentRoutes(storage: FlagStorage) {
    route("/api/experiments") {
        // GET /api/experiments - Get all experiments
        get {
            val experiments = storage.getAllExperiments()
            call.respond(experiments)
        }
        
        // GET /api/experiments/{key} - Get specific experiment
        get("{key}") {
            val key = call.parameters["key"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing experiment key")
            )
            
            val experiment = storage.getExperiment(key)
            if (experiment != null) {
                call.respond(mapOf(key to experiment))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Experiment not found"))
            }
        }
        
        // POST /api/experiments - Create new experiment
        post {
            try {
                val body = call.receive<Map<String, RestExperiment>>()
                if (body.size != 1) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Expected exactly one experiment key-value pair")
                    )
                    return@post
                }
                
                val (key, experiment) = body.entries.first()
                val created = storage.createExperiment(key, experiment)
                call.respond(HttpStatusCode.Created, mapOf(key to created))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.Conflict, mapOf(("error" to e.message) ?: "Conflict"))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid request body: ${e.message}")
                )
            }
        }
        
        // PUT /api/experiments/{key} - Update experiment
        put("{key}") {
            val key = call.parameters["key"] ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing experiment key")
            )
            
            try {
                val experiment = call.receive<RestExperiment>()
                val updated = storage.updateExperiment(key, experiment)
                if (updated != null) {
                    call.respond(mapOf(key to updated))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Experiment not found"))
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid request body: ${e.message}")
                )
            }
        }
        
        // DELETE /api/experiments/{key} - Delete experiment
        delete("{key}") {
            val key = call.parameters["key"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing experiment key")
            )
            
            val deleted = storage.deleteExperiment(key)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Experiment not found"))
            }
        }
    }
}

fun Routing.configRoutes(storage: FlagStorage) {
    route("/config") {
        // GET /config - Get full config (for client bootstrap)
        // GET /config?rev={revision} - Get config changes since revision
        get {
            val revision = call.request.queryParameters["rev"]
            val config = storage.getConfig(revision)
            call.respond(config)
        }
    }
}

