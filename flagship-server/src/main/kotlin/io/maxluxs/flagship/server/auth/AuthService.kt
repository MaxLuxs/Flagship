package io.maxluxs.flagship.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.maxluxs.flagship.server.database.models.Users
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import org.postgresql.util.PSQLException
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AuthService(
    private val jwtSecret: String = System.getenv("JWT_SECRET")
        ?: "change-me-in-production-use-strong-secret-key",
    private val jwtIssuer: String = "flagship-server",
    private val jwtAudience: String = "flagship-client"
) {
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    private val rateLimitAttempts = ConcurrentHashMap<String, MutableList<Long>>()
    private val rateLimitMutex = Mutex()
    private val maxAttempts = 5
    private val rateLimitWindowMs = 15 * 60 * 1000L

    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.checkpw(password, hash)
    }

    fun generateToken(userId: UUID, email: String, isAdmin: Boolean): String {
        return JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId.toString())
            .withClaim("email", email)
            .withClaim("isAdmin", isAdmin)
            .withExpiresAt(Date(System.currentTimeMillis() + 86400000)) // 24 hours
            .sign(algorithm)
    }

    fun verifyToken(token: String): TokenPayload? {
        return try {
            val verifier = JWT.require(algorithm)
                .withIssuer(jwtIssuer)
                .withAudience(jwtAudience)
                .build()

            val decoded = verifier.verify(token)
            TokenPayload(
                userId = UUID.fromString(decoded.getClaim("userId").asString()),
                email = decoded.getClaim("email").asString(),
                isAdmin = decoded.getClaim("isAdmin").asBoolean()
            )
        } catch (e: JWTVerificationException) {
            logger.debug("JWT verification failed: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            logger.debug("Invalid JWT token format: ${e.message}")
            null
        } catch (e: Exception) {
            logger.warn("Unexpected error during token verification: ${e.message}", e)
            null
        }
    }

    suspend fun authenticate(email: String, password: String): AuthResult? {
        if (!checkRateLimit(email)) {
            logger.warn("Rate limit exceeded for email: $email")
            return null
        }

        return try {
            val result = transaction {
                val user = Users.selectAll().where { Users.email eq email }.firstOrNull()
                if (user == null) {
                    return@transaction null
                }

                val passwordHash = user[Users.passwordHash]
                if (!verifyPassword(password, passwordHash)) {
                    return@transaction null
                }

                AuthResult(
                    userId = user[Users.id].value,
                    email = user[Users.email],
                    name = user[Users.name],
                    isAdmin = user[Users.isAdmin],
                    token = generateToken(
                        user[Users.id].value,
                        user[Users.email],
                        user[Users.isAdmin]
                    )
                )
            }

            if (result == null) {
                recordFailedAttempt(email)
                logger.debug("Authentication failed for email: $email")
                return null
            }

            clearFailedAttempts(email)
            logger.info("User authenticated successfully: ${result.email}")
            result
        } catch (e: PSQLException) {
            logger.error("Database error during authentication for email: $email", e)
            null
        } catch (e: Exception) {
            logger.error("Unexpected error during authentication for email: $email", e)
            null
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun register(email: String, password: String, name: String? = null): AuthResult? {
        if (!checkRateLimit(email)) {
            logger.warn("Rate limit exceeded for registration email: $email")
            return null
        }

        return try {
            val result = transaction {
                val existing = Users.selectAll().where { Users.email eq email }.firstOrNull()
                if (existing != null) {
                    return@transaction null
                }

                val now = Clock.System.now()
                val userId = Users.insert {
                    it[Users.email] = email
                    it[Users.passwordHash] = hashPassword(password)
                    it[Users.name] = name
                    it[Users.isAdmin] = false
                    it[Users.createdAt] = now
                    it[Users.updatedAt] = now
                } get Users.id

                AuthResult(
                    userId = userId.value,
                    email = email,
                    name = name,
                    isAdmin = false,
                    token = generateToken(userId.value, email, false)
                )
            }

            if (result == null) {
                logger.debug("Registration failed: user already exists for email: $email")
                return null
            }

            clearFailedAttempts(email)
            logger.info("User registered successfully: $email")
            result
        } catch (e: PSQLException) {
            logger.error("Database error during registration for email: $email", e)
            null
        } catch (e: Exception) {
            logger.error("Unexpected error during registration for email: $email", e)
            null
        }
    }

    private suspend fun checkRateLimit(identifier: String): Boolean {
        return rateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val attempts = rateLimitAttempts.getOrDefault(identifier, mutableListOf())
            attempts.removeAll { it < now - rateLimitWindowMs }
            attempts.size < maxAttempts
        }
    }

    private suspend fun recordFailedAttempt(identifier: String) {
        rateLimitMutex.withLock {
            val attempts = rateLimitAttempts.getOrDefault(identifier, mutableListOf())
            attempts.add(System.currentTimeMillis())
            rateLimitAttempts[identifier] = attempts
        }
    }

    private suspend fun clearFailedAttempts(identifier: String) {
        rateLimitMutex.withLock {
            rateLimitAttempts.remove(identifier)
        }
    }
}

data class TokenPayload(
    val userId: UUID,
    val email: String,
    val isAdmin: Boolean
)

data class AuthResult(
    val userId: UUID,
    val email: String,
    val name: String?,
    val isAdmin: Boolean,
    val token: String
)

