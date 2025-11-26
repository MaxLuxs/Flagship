package io.maxluxs.flagship.server.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.maxluxs.flagship.server.*
import io.maxluxs.flagship.server.auth.AuthService
import io.maxluxs.flagship.server.database.models.ProjectRole
import io.maxluxs.flagship.server.storage.InMemoryStorage
import io.maxluxs.flagship.shared.api.*
import kotlin.test.*
import kotlinx.serialization.json.*
import java.util.*

class ProjectRoutesTest {
    
    @Test
    fun testGetFlagsWithoutAuth() = testApplication {
        application {
            testModule()
        }
        
        val response = client.get("/api/projects/${UUID.randomUUID()}/flags")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
    
    @Test
    fun testGetFlagsEmpty() = testApplication {
        val storage = InMemoryStorage()
        val authService = AuthService()
        val accessService = MockProjectAccessService()
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        
        accessService.allowAccess(userId, projectId, ProjectRole.OWNER)
        val token = authService.generateToken(userId, "test@example.com", false)
        
        application {
            testModule(storage, authService, accessService = accessService)
        }
        
        val response = client.get("/api/projects/$projectId/flags") {
            header("Authorization", "Bearer $token")
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<Map<String, JsonElement>>(response.bodyAsText())
        assertTrue(body.isEmpty())
    }
    
    @Test
    fun testCreateFlag() = testApplication {
        val storage = InMemoryStorage()
        val authService = AuthService()
        val accessService = MockProjectAccessService()
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        
        accessService.allowAccess(userId, projectId, ProjectRole.ADMIN)
        val token = authService.generateToken(userId, "test@example.com", false)
        
        application {
            testModule(storage, authService, accessService = accessService)
        }
        
        val flagValue = RestFlagValue(type = "bool", value = JsonPrimitive(true))
        val request = mapOf("test_flag" to flagValue)
        
        val response = client.post("/api/projects/$projectId/flags") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(JsonObject.serializer(), JsonObject(request.mapValues { Json.encodeToJsonElement(RestFlagValue.serializer(), it.value) })))
        }
        
        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.decodeFromString<Map<String, JsonElement>>(response.bodyAsText())
        assertTrue(body.containsKey("test_flag"))
    }
    
    @Test
    fun testCreateFlagInvalidKey() = testApplication {
        val authService = AuthService()
        val accessService = MockProjectAccessService()
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        
        accessService.allowAccess(userId, projectId, ProjectRole.ADMIN)
        val token = authService.generateToken(userId, "test@example.com", false)
        
        application {
            testModule(accessService = accessService)
        }
        
        val flagValue = RestFlagValue(type = "bool", value = JsonPrimitive(true))
        val request = mapOf("invalid key with spaces!" to flagValue)
        
        val response = client.post("/api/projects/$projectId/flags") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(JsonObject.serializer(), JsonObject(request.mapValues { Json.encodeToJsonElement(RestFlagValue.serializer(), it.value) })))
        }
        
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
    
    @Test
    fun testGetFlag() = testApplication {
        val storage = InMemoryStorage()
        val authService = AuthService()
        val accessService = MockProjectAccessService()
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        
        accessService.allowAccess(userId, projectId, ProjectRole.OWNER)
        val token = authService.generateToken(userId, "test@example.com", false)
        
        // Create flag first
        val flagValue = RestFlagValue(type = "bool", value = JsonPrimitive(true))
        storage.createFlag(projectId, "test_flag", flagValue, userId)
        
        application {
            testModule(storage, authService, accessService = accessService)
        }
        
        val response = client.get("/api/projects/$projectId/flags/test_flag") {
            header("Authorization", "Bearer $token")
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<Map<String, JsonElement>>(response.bodyAsText())
        assertTrue(body.containsKey("test_flag"))
    }
    
    @Test
    fun testGetFlagNotFound() = testApplication {
        val authService = AuthService()
        val accessService = MockProjectAccessService()
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        
        accessService.allowAccess(userId, projectId, ProjectRole.OWNER)
        val token = authService.generateToken(userId, "test@example.com", false)
        
        application {
            testModule(accessService = accessService)
        }
        
        val response = client.get("/api/projects/$projectId/flags/nonexistent") {
            header("Authorization", "Bearer $token")
        }
        
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
    
    @Test
    fun testUpdateFlag() = testApplication {
        val storage = InMemoryStorage()
        val authService = AuthService()
        val accessService = MockProjectAccessService()
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        
        accessService.allowAccess(userId, projectId, ProjectRole.ADMIN)
        val token = authService.generateToken(userId, "test@example.com", false)
        
        // Create flag first
        val flagValue = RestFlagValue(type = "bool", value = JsonPrimitive(true))
        storage.createFlag(projectId, "test_flag", flagValue, userId)
        
        application {
            testModule(storage, authService, accessService = accessService)
        }
        
        // Update flag
        val updatedValue = RestFlagValue(type = "bool", value = JsonPrimitive(false))
        val response = client.put("/api/projects/$projectId/flags/test_flag") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RestFlagValue.serializer(), updatedValue))
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<Map<String, JsonElement>>(response.bodyAsText())
        assertTrue(body.containsKey("test_flag"))
    }
    
    @Test
    fun testDeleteFlag() = testApplication {
        val storage = InMemoryStorage()
        val authService = AuthService()
        val accessService = MockProjectAccessService()
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        
        accessService.allowAccess(userId, projectId, ProjectRole.ADMIN)
        val token = authService.generateToken(userId, "test@example.com", false)
        
        // Create flag first
        val flagValue = RestFlagValue(type = "bool", value = JsonPrimitive(true))
        storage.createFlag(projectId, "test_flag", flagValue, userId)
        
        application {
            testModule(storage, authService, accessService = accessService)
        }
        
        val response = client.delete("/api/projects/$projectId/flags/test_flag") {
            header("Authorization", "Bearer $token")
        }
        
        assertEquals(HttpStatusCode.NoContent, response.status)
    }
    
    @Test
    fun testGetExperiments() = testApplication {
        val authService = AuthService()
        val accessService = MockProjectAccessService()
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        
        accessService.allowAccess(userId, projectId, ProjectRole.OWNER)
        val token = authService.generateToken(userId, "test@example.com", false)
        
        application {
            testModule(accessService = accessService)
        }
        
        val response = client.get("/api/projects/$projectId/experiments") {
            header("Authorization", "Bearer $token")
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<Map<String, JsonElement>>(response.bodyAsText())
        assertTrue(body.isEmpty())
    }
    
    @Test
    fun testGetConfig() = testApplication {
        val storage = InMemoryStorage()
        val authService = AuthService()
        val accessService = MockProjectAccessService()
        val userId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        
        accessService.allowAccess(userId, projectId, ProjectRole.OWNER)
        val token = authService.generateToken(userId, "test@example.com", false)
        
        // Create a flag first
        val flagValue = RestFlagValue(type = "bool", value = JsonPrimitive(true))
        storage.createFlag(projectId, "test_flag", flagValue, userId)
        
        application {
            testModule(storage, authService, accessService = accessService)
        }
        
        val response = client.get("/api/projects/$projectId/config") {
            header("Authorization", "Bearer $token")
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<RestResponse>(response.bodyAsText())
        assertTrue(body.flags.containsKey("test_flag"))
        assertNotNull(body.revision)
    }
}
