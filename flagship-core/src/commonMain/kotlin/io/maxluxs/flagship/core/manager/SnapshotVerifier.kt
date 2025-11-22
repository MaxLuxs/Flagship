package io.maxluxs.flagship.core.manager

import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.security.SignatureValidator
import io.maxluxs.flagship.core.util.FlagsLogger

/**
 * Utility for verifying provider snapshots.
 * 
 * Centralizes signature verification logic to avoid duplication
 * across bootstrap, refresh, and cache loading.
 */
class SnapshotVerifier(
    private val signatureValidator: SignatureValidator?,
    private val logger: FlagsLogger
) {
    /**
     * Verify snapshot signature if present.
     * 
     * @param snapshot The snapshot to verify
     * @param providerName Provider name for logging
     * @return true if valid or no signature present, false if invalid
     */
    fun verify(snapshot: ProviderSnapshot, providerName: String): Boolean {
        if (snapshot.signature == null || signatureValidator == null) {
            return true // No signature to verify
        }
        
        val isValid = signatureValidator.verifyHash(snapshot, snapshot.signature)
        if (!isValid) {
            logger.error("FlagsManager", "Invalid signature for $providerName")
            return false
        }
        
        return true
    }
    
    /**
     * Verify snapshot and throw exception if invalid.
     * 
     * @param snapshot The snapshot to verify
     * @param providerName Provider name for logging
     * @throws Exception if signature is invalid
     */
    fun verifyOrThrow(snapshot: ProviderSnapshot, providerName: String) {
        if (!verify(snapshot, providerName)) {
            throw Exception("Invalid snapshot signature for $providerName")
        }
    }
}

