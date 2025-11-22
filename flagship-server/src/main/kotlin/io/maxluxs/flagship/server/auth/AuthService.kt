package io.maxluxs.flagship.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt
import io.maxluxs.flagship.server.database.models.Users
import kotlinx.datetime.Clock
import java.util.*

class AuthService(
    private val jwtSecret: String = System.getenv("JWT_SECRET") ?: "change-me-in-production-use-strong-secret-key",
    private val jwtIssuer: String = "flagship-server",
    private val jwtAudience: String = "flagship-client"
) {
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    
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
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun authenticate(email: String, password: String): AuthResult? {
        return transaction {
            val user = Users.select { Users.email eq email }.firstOrNull()
            if (user == null) return@transaction null
            
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
    }
    
    suspend fun register(email: String, password: String, name: String? = null): AuthResult? {
        return transaction {
            // Check if user exists
            val existing = Users.select { Users.email eq email }.firstOrNull()
            if (existing != null) return@transaction null
            
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

