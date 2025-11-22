package io.maxluxs.flagship.server.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

fun Application.configureAuth(authService: AuthService) {
    val jwtSecret = System.getenv("JWT_SECRET") ?: "change-me-in-production-use-strong-secret-key"
    val algorithm = Algorithm.HMAC256(jwtSecret)
    
    install(Authentication) {
        jwt("jwt-auth") {
            realm = "Flagship API"
            verifier(
                JWT.require(algorithm)
                    .withIssuer("flagship-server")
                    .withAudience("flagship-client")
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                val email = credential.payload.getClaim("email").asString()
                val isAdmin = credential.payload.getClaim("isAdmin").asBoolean()
                
                if (userId != null && email != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}

suspend fun ApplicationCall.getUserId(): String? {
    return principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
}

suspend fun ApplicationCall.getUserEmail(): String? {
    return principal<JWTPrincipal>()?.payload?.getClaim("email")?.asString()
}

suspend fun ApplicationCall.isAdmin(): Boolean {
    return principal<JWTPrincipal>()?.payload?.getClaim("isAdmin")?.asBoolean() ?: false
}

fun ApplicationCall.requireAuth(): JWTPrincipal {
    return principal<JWTPrincipal>() 
        ?: throw io.ktor.server.plugins.BadRequestException("Authentication required")
}

