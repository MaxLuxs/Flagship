package io.maxluxs.flagship.server.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val name: String?,
    val isAdmin: Boolean
)

fun Routing.authRoutes(authService: AuthService) {
    route("/api/auth") {
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
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
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid request: ${e.message}")
                )
            }
        }
        
        post("/register") {
            try {
                val request = call.receive<RegisterRequest>()
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
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid request: ${e.message}")
                )
            }
        }
    }
}

