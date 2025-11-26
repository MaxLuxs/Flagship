package io.maxluxs.flagship.server.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.maxluxs.flagship.server.*
import kotlin.test.*
import kotlinx.serialization.json.*

class HealthRoutesTest {
    
    @Test
    fun testHealthEndpoint() = testApplication {
        application {
            testModule()
        }
        
        val response = client.get("/health")
        
        // Health endpoint may return OK or ServiceUnavailable depending on DB connection
        assertTrue(response.status == HttpStatusCode.OK || response.status == HttpStatusCode.ServiceUnavailable)
        val body = Json.decodeFromString<Map<String, JsonElement>>(response.bodyAsText())
        assertTrue(body.containsKey("status"))
    }
    
    @Test
    fun testLivenessEndpoint() = testApplication {
        application {
            testModule()
        }
        
        val response = client.get("/health/liveness")
        
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<Map<String, JsonElement>>(response.bodyAsText())
        assertEquals("alive", body["status"]?.jsonPrimitive?.content)
    }
    
    @Test
    fun testReadinessEndpoint() = testApplication {
        application {
            testModule()
        }
        
        val response = client.get("/health/readiness")
        
        // Readiness may return OK or ServiceUnavailable depending on DB
        assertTrue(response.status == HttpStatusCode.OK || response.status == HttpStatusCode.ServiceUnavailable)
        val body = Json.decodeFromString<Map<String, JsonElement>>(response.bodyAsText())
        assertTrue(body.containsKey("status"))
    }
}
