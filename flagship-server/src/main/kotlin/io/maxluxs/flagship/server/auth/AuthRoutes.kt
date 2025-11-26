package io.maxluxs.flagship.server.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.maxluxs.flagship.server.database.models.Users
import io.maxluxs.flagship.server.util.ValidationUtils
import io.maxluxs.flagship.shared.api.AuthResponse
import io.maxluxs.flagship.shared.api.LoginRequest
import io.maxluxs.flagship.shared.api.RegisterRequest
import io.maxluxs.flagship.shared.api.UpdateUserRequest
import io.maxluxs.flagship.shared.api.UserResponse
import kotlinx.serialization.SerializationException
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun Routing.authRoutes(authService: AuthService) {
    route("/api/auth") {
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()

                val emailValidation = ValidationUtils.validateEmailWithResult(request.email)
                if (!emailValidation.isValid) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to emailValidation.error)
                    )
                    return@post
                }

                val result = authService.authenticate(request.email, request.password)

                if (result == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "Invalid email or password")
                    )
                    return@post
                }

                call.respond(
                    AuthResponse(
                        token = result.token,
                        user = UserResponse(
                            id = result.userId.toString(),
                            email = result.email,
                            name = result.name,
                            isAdmin = result.isAdmin
                        )
                    )
                )
            } catch (e: BadRequestException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid request: ${e.message}")
                )
            } catch (_: SerializationException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid request format")
                )
            } catch (_: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Internal server error")
                )
            }
        }

        post("/register") {
            try {
                val request = call.receive<RegisterRequest>()

                val emailValidation = ValidationUtils.validateEmailWithResult(request.email)
                if (!emailValidation.isValid) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to emailValidation.error)
                    )
                    return@post
                }

                val passwordValidation = ValidationUtils.validatePassword(request.password)
                if (!passwordValidation.isValid) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to passwordValidation.error)
                    )
                    return@post
                }

                val result = authService.register(request.email, request.password, request.name)

                if (result == null) {
                    call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "User with this email already exists")
                    )
                    return@post
                }

                call.respond(
                    HttpStatusCode.Created,
                    AuthResponse(
                        token = result.token,
                        user = UserResponse(
                            id = result.userId.toString(),
                            email = result.email,
                            name = result.name,
                            isAdmin = result.isAdmin
                        )
                    )
                )
            } catch (e: BadRequestException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid request: ${e.message}")
                )
            } catch (_: SerializationException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid request format")
                )
            } catch (_: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Internal server error")
                )
            }
        }

        authenticate("jwt-auth") {
            route("/user") {
                get {
                    val userId = UUID.fromString(
                        call.getUserId() ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "Unauthorized")
                        )
                    )

                    try {
                        val user = transaction {
                            Users.selectAll().where { Users.id eq userId }.firstOrNull()
                        }

                        if (user == null) {
                            return@get call.respond(
                                HttpStatusCode.NotFound,
                                mapOf("error" to "User not found")
                            )
                        }

                        call.respond(
                            UserResponse(
                                id = user[Users.id].value.toString(),
                                email = user[Users.email],
                                name = user[Users.name],
                                isAdmin = user[Users.isAdmin]
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to retrieve user: ${e.message}")
                        )
                    }
                }

                put {
                    val userId = UUID.fromString(
                        call.getUserId() ?: return@put call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "Unauthorized")
                        )
                    )

                    try {
                        val request = call.receive<UpdateUserRequest>()
                        val nameValue = request.name

                        if (nameValue != null && (nameValue.isBlank() || nameValue.length > 255)) {
                            return@put call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Name must be between 1 and 255 characters")
                            )
                        }

                        val updatedUser = transaction {
                            Users.update({ Users.id eq userId }) {
                                if (request.name != null) {
                                    it[name] = request.name
                                }
                                it[updatedAt] = Clock.System.now()
                            }

                            Users.selectAll().where { Users.id eq userId }.first().let { row ->
                                UserResponse(
                                    id = row[Users.id].value.toString(),
                                    email = row[Users.email],
                                    name = row[Users.name],
                                    isAdmin = row[Users.isAdmin]
                                )
                            }
                        }

                        call.respond(updatedUser)
                    } catch (e: SerializationException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Invalid request body format: ${e.message}")
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to update user: ${e.message}")
                        )
                    }
                }
            }
        }
    }
}

