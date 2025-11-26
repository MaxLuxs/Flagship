package io.maxluxs.flagship.core.performance

import io.maxluxs.flagship.core.util.currentTimeMillis

import io.maxluxs.flagship.core.util.currentTimeMillis

import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.util.FlagsLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Memory optimizer for Flagship.
 * 
 * Provides utilities for optimizing memory usage:
 * - Cleaning up unused snapshots
 * - Compacting data structures
 * - Managing cache size
 */
class MemoryOptimizer(
    private val logger: FlagsLogger,
    private val maxSnapshots: Int = 10,
    private val maxSnapshotAgeMs: Long = 24 * 60 * 60 * 1000 // 24 hours
) {
    private val mutex = Mutex()
    
    /**
     * Clean up old or unused snapshots.
     * 
     * @param snapshots Map of provider names to snapshots
     * @return Map with cleaned snapshots
     */
    suspend fun cleanupSnapshots(
        snapshots: Map<String, ProviderSnapshot>
    ): Map<String, ProviderSnapshot> {
        return mutex.withLock {
            val currentTime = currentTimeMillis()
            val cleaned = snapshots.filter { (_, snapshot) ->
                val age = currentTime - snapshot.fetchedAtMs
                age < maxSnapshotAgeMs
            }
            
            if (cleaned.size < snapshots.size) {
                val removed = snapshots.size - cleaned.size
                logger.info("MemoryOptimizer", "Cleaned up $removed old snapshot(s)")
            }
            
            // If still too many, keep only the most recent
            if (cleaned.size > maxSnapshots) {
                val sorted = cleaned.toList().sortedByDescending { (_, snapshot) ->
                    snapshot.fetchedAtMs
                }
                val kept = sorted.take(maxSnapshots).toMap()
                val removed = cleaned.size - kept.size
                logger.info("MemoryOptimizer", "Removed $removed excess snapshot(s)")
                kept
            } else {
                cleaned
            }
        }
    }
    
    /**
     * Compact a snapshot by removing unused data.
     * 
     * Currently, snapshots are immutable, so this is a no-op.
     * In the future, could remove unused flags/experiments.
     */
    fun compactSnapshot(snapshot: ProviderSnapshot): ProviderSnapshot {
        // Snapshots are immutable, so we return as-is
        // In the future, could implement compression or removal of unused flags
        return snapshot
    }
    
    /**
     * Get memory usage estimate for snapshots.
     */
    fun estimateMemoryUsage(snapshots: Map<String, ProviderSnapshot>): MemoryUsage {
        var totalFlags = 0
        var totalExperiments = 0
        var totalSize = 0L
        
        snapshots.values.forEach { snapshot ->
            totalFlags += snapshot.flags.size
            totalExperiments += snapshot.experiments.size
            // Rough estimate: each flag ~100 bytes, each experiment ~200 bytes
            totalSize += snapshot.flags.size * 100L
            totalSize += snapshot.experiments.size * 200L
        }
        
        return MemoryUsage(
            snapshotCount = snapshots.size,
            totalFlags = totalFlags,
            totalExperiments = totalExperiments,
            estimatedSizeBytes = totalSize
        )
    }
    
    /**
     * Suggest optimizations based on current state.
     */
    suspend fun suggestOptimizations(
        snapshots: Map<String, ProviderSnapshot>
    ): List<String> {
        val suggestions = mutableListOf<String>()
        val usage = estimateMemoryUsage(snapshots)
        
        if (snapshots.size > maxSnapshots) {
            suggestions.add("Too many snapshots (${snapshots.size} > $maxSnapshots). Consider cleaning up old snapshots.")
        }
        
        if (usage.estimatedSizeBytes > 10 * 1024 * 1024) { // 10MB
            suggestions.add("Large memory usage (${usage.estimatedSizeBytes / 1024 / 1024}MB). Consider reducing number of flags/experiments.")
        }
        
        val oldSnapshots = snapshots.values.count { snapshot ->
            val age = currentTimeMillis() - snapshot.fetchedAtMs
            age > maxSnapshotAgeMs
        }
        
        if (oldSnapshots > 0) {
            suggestions.add("$oldSnapshots old snapshot(s) found. Consider cleaning up.")
        }
        
        return suggestions
    }
}

/**
 * Memory usage statistics.
 */
data class MemoryUsage(
    val snapshotCount: Int,
    val totalFlags: Int,
    val totalExperiments: Int,
    val estimatedSizeBytes: Long
)

