package io.maxluxs.flagship.core.security

import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.util.Crypto
import io.maxluxs.flagship.core.util.FlagsSerializer

/**
 * Validates signatures on provider snapshots.
 * 
 * Ensures that snapshots haven't been tampered with by verifying
 * cryptographic signatures.
 */
class SignatureValidator(
    private val crypto: Crypto,
    private val serializer: FlagsSerializer
) {
    /**
     * Verify signature on a snapshot.
     * 
     * @param snapshot The snapshot to verify
     * @param signature The signature to verify against
     * @return true if signature is valid, false otherwise
     */
    fun verify(snapshot: ProviderSnapshot, signature: String): Boolean {
        val data = serializer.serialize(snapshot)
        return crypto.verify(data, signature)
    }
    
    /**
     * Verify hash-based integrity check (simpler alternative to crypto signatures).
     * 
     * @param snapshot The snapshot to verify
     * @param expectedHash The expected hash value
     * @return true if hash matches, false otherwise
     */
    fun verifyHash(snapshot: ProviderSnapshot, expectedHash: String): Boolean {
        val data = serializer.serialize(snapshot)
        val actualHash = hashString(data)
        return actualHash == expectedHash
    }
    
    /**
     * Calculate hash for a snapshot.
     */
    fun calculateHash(snapshot: ProviderSnapshot): String {
        val data = serializer.serialize(snapshot)
        return hashString(data)
    }
    
    private fun hashString(data: String): String {
        // Simple hash function (in production, use SHA-256 or similar)
        var hash = 0L
        for (char in data) {
            hash = ((hash shl 5) - hash) + char.code.toLong()
            hash = hash and hash // Convert to 32bit
        }
        return hash.toString(16)
    }
}

/**
 * No-op validator that always returns true (for development only).
 */
object NoopSignatureValidator {
    fun verify(snapshot: ProviderSnapshot, signature: String): Boolean = true
    fun verifyHash(snapshot: ProviderSnapshot, expectedHash: String): Boolean = true
    fun calculateHash(snapshot: ProviderSnapshot): String = "noop-hash"
}

