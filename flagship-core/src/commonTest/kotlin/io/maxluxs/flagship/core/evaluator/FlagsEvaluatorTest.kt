package io.maxluxs.flagship.core.evaluator

import io.maxluxs.flagship.core.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class FlagsEvaluatorTest {
    private val evaluator = FlagsEvaluator()

    @Test
    fun testEvaluateFlagWithOverride() {
        val overrides = mapOf("flag1" to FlagValue.Bool(true))
        val snapshots = listOf(
            ProviderSnapshot(
                flags = mapOf("flag1" to FlagValue.Bool(false)),
                fetchedAtMs = 0
            )
        )

        val result = evaluator.evaluateFlag("flag1", overrides, snapshots, null)
        assertEquals(FlagValue.Bool(true), result)
    }

    @Test
    fun testEvaluateFlagWithoutOverride() {
        val overrides = emptyMap<String, FlagValue>()
        val snapshots = listOf(
            ProviderSnapshot(
                flags = mapOf("flag1" to FlagValue.Bool(false)),
                fetchedAtMs = 0
            )
        )

        val result = evaluator.evaluateFlag("flag1", overrides, snapshots, null)
        assertEquals(FlagValue.Bool(false), result)
    }

    @Test
    fun testEvaluateFlagWithDefault() {
        val overrides = emptyMap<String, FlagValue>()
        val snapshots = emptyList<ProviderSnapshot>()
        val default = FlagValue.Bool(true)

        val result = evaluator.evaluateFlag("flag1", overrides, snapshots, default)
        assertEquals(default, result)
    }

    @Test
    fun testProviderPrecedence() {
        val overrides = emptyMap<String, FlagValue>()
        val snapshots = listOf(
            ProviderSnapshot(
                flags = mapOf("flag1" to FlagValue.Bool(true)),
                fetchedAtMs = 0
            ),
            ProviderSnapshot(
                flags = mapOf("flag1" to FlagValue.Bool(false)),
                fetchedAtMs = 0
            )
        )

        // First provider should win
        val result = evaluator.evaluateFlag("flag1", overrides, snapshots, null)
        assertEquals(FlagValue.Bool(true), result)
    }

    @Test
    fun testIsSnapshotExpired() {
        val snapshot = ProviderSnapshot(
            fetchedAtMs = 1000,
            ttlMs = 5000
        )

        // Not expired
        assertEquals(false, evaluator.isSnapshotExpired(snapshot, 5000))

        // Expired
        assertEquals(true, evaluator.isSnapshotExpired(snapshot, 7000))

        // No TTL means never expires
        val snapshotNoTTL = ProviderSnapshot(
            fetchedAtMs = 1000,
            ttlMs = null
        )
        assertEquals(false, evaluator.isSnapshotExpired(snapshotNoTTL, 10000000))
    }
}

