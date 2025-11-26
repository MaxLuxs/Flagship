package io.maxluxs.flagship.server.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import org.slf4j.LoggerFactory

fun Application.configureAuth(authService: AuthService) {
    val jwtSecret = System.getenv("JWT_SECRET") ?: "change-me-in-production-use-strong-secret-key"
    val algorithm = Algorithm.HMAC256(jwtSecret)
    val logger = LoggerFactory.getLogger("AuthMiddleware")
    
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
                try {
                    val exp = credential.payload.expiresAt
                    if (exp != null && exp.before(java.util.Date())) {
                        logger.debug("Token expired for user")
                        return@validate null
                    }
                    
                    val userId = credential.payload.getClaim("userId")?.asString()
                    val email = credential.payload.getClaim("email")?.asString()
                    
                    if (userId != null && email != null) {
                        JWTPrincipal(credential.payload)
                    } else {
                        logger.debug("Token missing required claims (userId or email)")
                        null
                    }
                } catch (e: TokenExpiredException) {
                    logger.debug("Token expired: ${e.message}")
                    null
                } catch (e: JWTVerificationException) {
                    logger.debug("JWT verification failed: ${e.message}")
                    null
                } catch (e: Exception) {
                    logger.warn("Unexpected error during token validation: ${e.message}", e)
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Token expired or invalid. Please login again.")
                )
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
        ?: throw io.ktor.server.plugins.BadRequestException("Authentication required. Please provide a valid token.")
}

