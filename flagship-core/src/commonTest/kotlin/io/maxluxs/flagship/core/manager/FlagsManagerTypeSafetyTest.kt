package io.maxluxs.flagship.core.manager

import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.InMemoryCache
import io.maxluxs.flagship.core.model.EvalContext
import io.maxluxs.flagship.core.model.FlagKey
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.model.ExperimentAssignment
import io.maxluxs.flagship.core.model.ExperimentKey
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlagsManagerTypeSafetyTest {

    private val mockProvider = object : FlagsProvider {
        override val name = "mock"
        
        override suspend fun bootstrap(): ProviderSnapshot {
            return ProviderSnapshot(
                flags = mapOf(
                    "int_flag" to FlagValue.Int(42),
                    "string_flag" to FlagValue.StringV("hello"),
                    "bool_flag" to FlagValue.Bool(true),
                    "double_flag" to FlagValue.Double(3.14)
                ),
                experiments = emptyMap(),
                revision = "1",
                fetchedAtMs = 0
            )
        }
        
        override suspend fun refresh(): ProviderSnapshot = bootstrap()
        override fun evaluateFlag(key: FlagKey, context: EvalContext): FlagValue? = null
        override fun evaluateExperiment(key: ExperimentKey, context: EvalContext): ExperimentAssignment? = null
    }

    @Test
    fun `test correct type retrieval`() = runTest {
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(mockProvider),
            cache = InMemoryCache()
        )
        val manager = DefaultFlagsManager(config)
        manager.ensureBootstrap()

        assertEquals(42, manager.value("int_flag", 0))
        assertEquals("hello", manager.value("string_flag", "default"))
        assertEquals(true, manager.value("bool_flag", false))
        assertEquals(3.14, manager.value("double_flag", 0.0))
    }

    @Test
    fun `test type mismatch fallback`() = runTest {
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(mockProvider),
            cache = InMemoryCache()
        )
        val manager = DefaultFlagsManager(config)
        manager.ensureBootstrap()

        // Asking for String but flag is Int -> Should return Default
        // Note: The implementation casts checking the 'default' type.
        // If stored is Int(42) and we ask value("int_flag", "default") -> 
        // Implementation: evaluateInternal returns Int(42).
        // Then 'when(default)' matches String -> returns (value.asString() ?: default)
        // FlagValue.Int does NOT automatically convert to String in current impl?
        // Let's check FlagValue implementation.
        
        val result = manager.value("int_flag", "default_string")
        // We expect "default_string" because Int(42) cannot be cast to String safely without explicit conversion logic
        // Or strict type check failure.
        
        assertEquals("default_string", result)
    }
    
    @Test
    fun `test missing flag fallback`() = runTest {
        val config = FlagsConfig(
            appKey = "test",
            environment = "test",
            providers = listOf(mockProvider),
            cache = InMemoryCache()
        )
        val manager = DefaultFlagsManager(config)
        manager.ensureBootstrap()

        assertEquals("missing", manager.value("non_existent", "missing"))
    }
}

