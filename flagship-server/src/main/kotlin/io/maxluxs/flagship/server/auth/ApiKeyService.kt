package io.maxluxs.flagship.server.auth

import io.maxluxs.flagship.server.database.models.ApiKeyType
import io.maxluxs.flagship.server.database.models.ApiKeys
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ApiKeyService {
    private val logger = LoggerFactory.getLogger(ApiKeyService::class.java)

    data class ApiKeyInfo(
        val projectId: UUID,
        val type: ApiKeyType,
        val keyId: UUID
    )

    @OptIn(ExperimentalTime::class)
    fun verifyApiKey(apiKey: String): ApiKeyInfo? {
        if (apiKey.length < 20) {
            return null
        }

        return transaction {
            val allKeys =
                ApiKeys.selectAll().where { ApiKeys.isActive eq true }.limit(1000).toList()

            for (row in allKeys) {
                val keyHash = row[ApiKeys.keyHash]
                try {
                    if (BCrypt.checkpw(apiKey, keyHash)) {
                        val expiresAt = row[ApiKeys.expiresAt]
                        val now = Clock.System.now()

                        if (expiresAt != null && expiresAt < now) {
                            logger.debug("API key expired: ${row[ApiKeys.id].value}")
                            continue
                        }

                        val projectId = row[ApiKeys.projectId]
                        val type = row[ApiKeys.type]
                        val keyId = row[ApiKeys.id].value

                        ApiKeys.update({ ApiKeys.id eq keyId }) {
                            it[ApiKeys.lastUsedAt] = now
                        }

                        return@transaction ApiKeyInfo(projectId, type, keyId)
                    }
                } catch (e: Exception) {
                    logger.debug("Error checking API key hash: ${e.message}")
                    continue
                }
            }

            null
        }
    }

    fun checkApiKeyAccess(
        apiKey: String,
        projectId: UUID,
        requiredType: ApiKeyType? = null
    ): Boolean {
        val keyInfo = verifyApiKey(apiKey) ?: return false

        if (keyInfo.projectId != projectId) {
            return false
        }

        if (requiredType != null) {
            return when (requiredType) {
                ApiKeyType.ADMIN -> keyInfo.type == ApiKeyType.ADMIN
                ApiKeyType.READ_ONLY -> true
            }
        }

        return true
    }
}

suspend fun io.ktor.server.application.ApplicationCall.getApiKey(): String? {
    val authHeader = request.headers["Authorization"]
    return when {
        authHeader?.startsWith("Bearer ") == true -> authHeader.substring(7)
        authHeader?.startsWith("ApiKey ") == true -> authHeader.substring(7)
        else -> null
    }
}

