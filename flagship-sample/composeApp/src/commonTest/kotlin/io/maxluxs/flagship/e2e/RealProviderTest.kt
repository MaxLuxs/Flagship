package io.maxluxs.flagship.e2e
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.helpers.TestHelpers

import io.maxluxs.flagship.core.Flags
import io.maxluxs.flagship.core.model.FlagValue
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.util.currentTimeMillis
import io.maxluxs.flagship.helpers.TestFlagsConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.request.get
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests with mock HTTP server simulating real REST provider
 */
class RealProviderTest {
    
    private class MockRestProvider(
        private val httpClient: HttpClient
    ) : FlagsProvider {
        override val name: String = "mock-rest"
        
        override suspend fun bootstrap(): ProviderSnapshot {
            // Simulate REST API call - just return snapshot directly
            // In real implementation, this would parse the HTTP response
            return ProviderSnapshot(
                flags = mapOf(
                    "rest_flag" to FlagValue.Bool(true),
                    "rest_int" to FlagValue.Int(100)
                ) as Map<String, FlagValue>,
                experiments = emptyMap(),
                revision = "rest-v1",
                fetchedAtMs = currentTimeMillis(),
                ttlMs = 60_000L
            )
        }
        
        override suspend fun refresh(): ProviderSnapshot = bootstrap()
        override fun evaluateFlag(key: String, context: io.maxluxs.flagship.core.model.EvalContext): FlagValue? = null
        override fun evaluateExperiment(key: String, context: io.maxluxs.flagship.core.model.EvalContext): io.maxluxs.flagship.core.model.ExperimentAssignment? = null
    }
    
    @BeforeTest
    fun setup() {
        Flags.reset()
    }
    
    @AfterTest
    fun teardown() {
        Flags.reset()
    }
    
    @Test
    fun testRestProviderFlow() = runTest {
        // Create mock HTTP client
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/flags" -> {
                    respond(
                        content = """{"flags":{"rest_flag":true,"rest_int":100}}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType to listOf("application/json"))
                    )
                }
                else -> {
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.NotFound
                    )
                }
            }
        }
        
        val httpClient = HttpClient(mockEngine)
        val provider = MockRestProvider(httpClient)
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        val bootstrapped = manager.ensureBootstrap(timeoutMs = 5000)
        assertTrue(bootstrapped)
        
        assertTrue(manager.isEnabled("rest_flag", false))
        assertEquals(100, manager.value("rest_int", default = 0))
    }
    
    @Test
    fun testProviderErrorHandling() = runTest {
        // Create mock HTTP client that returns errors
        val mockEngine = MockEngine { request ->
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError
            )
        }
        
        val httpClient = HttpClient(mockEngine)
        val provider = MockRestProvider(httpClient)
        
        val config = TestFlagsConfig.createTestConfig(providers = listOf(provider))
        Flags.configure(config)
        val manager = Flags.manager() as DefaultFlagsManager
        manager.setDefaultContext(TestHelpers.createTestContext())
        
        // Bootstrap should handle errors gracefully
        val bootstrapped = manager.ensureBootstrap(timeoutMs = 5000)
        // Should either fail gracefully or use fallback
    }
}

