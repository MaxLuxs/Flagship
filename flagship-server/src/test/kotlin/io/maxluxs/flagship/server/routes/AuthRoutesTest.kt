package io.maxluxs.flagship.server.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.maxluxs.flagship.server.*
import io.maxluxs.flagship.server.auth.AuthService
import io.maxluxs.flagship.shared.api.LoginRequest
import io.maxluxs.flagship.shared.api.RegisterRequest
import kotlin.test.*
import kotlinx.serialization.json.*
import java.util.*

class AuthRoutesTest {
    
    @Test
    fun testRegisterSuccess() = testApplication {
        application {
            testModule()
        }
        
        val request = RegisterRequest(
            email = "test@example.com",
            password = "TestPassword123!",
            name = "Test User"
        )
        
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterRequest.serializer(), request))
        }
        
        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.decodeFromString<Map<String, JsonElement>>(response.bodyAsText())
        assertTrue(body.containsKey("token"))
        assertTrue(body.containsKey("user"))
        assertEquals("test@example.com", body["user"]?.jsonObject?.get("email")?.jsonPrimitive?.content)
    }
    
    @Test
    fun testRegisterInvalidEmail() = testApplication {
        application {
            testModule()
        }
        
        val request = RegisterRequest(
            email = "invalid-email",
            password = "TestPassword123!",
            name = "Test User"
        )
        
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterRequest.serializer(), request))
        }
        
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
        assertTrue(body.containsKey("error"))
    }
    
    @Test
    fun testRegisterWeakPassword() = testApplication {
        application {
            testModule()
        }
        
        val request = RegisterRequest(
            email = "test@example.com",
            password = "123",
            name = "Test User"
        )
        
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterRequest.serializer(), request))
        }
        
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
        assertTrue(body.containsKey("error"))
    }
    
    @Test
    fun testRegisterDuplicateEmail() = testApplication {
        application {
            testModule()
        }
        
        val request = RegisterRequest(
            email = "test@example.com",
            password = "TestPassword123!",
            name = "Test User"
        )
        
        // First registration
        client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterRequest.serializer(), request))
        }
        
        // Second registration with same email
        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterRequest.serializer(), request))
        }
        
        assertEquals(HttpStatusCode.Conflict, response.status)
        val body = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
        assertTrue(body.containsKey("error"))
    }
    
    @Test
    fun testLoginSuccess() = testApplication {
        application {
            testModule()
        }
        
        val authService = AuthService()
        
        // Register first
        val registerRequest = RegisterRequest(
            email = "test@example.com",
            password = "TestPassword123!",
            name = "Test User"
        )
        
        client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterRequest.serializer(), registerRequest))
        }
        
        // Then login
        val loginRequest = LoginRequest(
            email = "test@example.com",
            password = "TestPassword123!"
        )
        
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(LoginRequest.serializer(), loginRequest))
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<Map<String, JsonElement>>(response.bodyAsText())
        assertTrue(body.containsKey("token"))
        assertTrue(body.containsKey("user"))
    }
    
    @Test
    fun testLoginInvalidCredentials() = testApplication {
        application {
            testModule()
        }
        
        val loginRequest = LoginRequest(
            email = "nonexistent@example.com",
            password = "WrongPassword123!"
        )
        
        val response = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(LoginRequest.serializer(), loginRequest))
        }
        
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
        assertTrue(body.containsKey("error"))
    }
    
    @Test
    fun testGetUserWithoutAuth() = testApplication {
        application {
            testModule()
        }
        
        val response = client.get("/api/auth/user")
        
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
    
    @Test
    fun testGetUserWithAuth() = testApplication {
        application {
            testModule()
        }
        
        val authService = AuthService()
        
        // Register and get token
        val registerRequest = RegisterRequest(
            email = "test@example.com",
            password = "TestPassword123!",
            name = "Test User"
        )
        
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterRequest.serializer(), registerRequest))
        }
        
        val registerBody = Json.decodeFromString<Map<String, JsonElement>>(registerResponse.bodyAsText())
        val token = registerBody["token"]?.jsonPrimitive?.content ?: fail("Token not found")
        val userId = registerBody["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content ?: fail("User ID not found")
        
        // Get user info
        val response = client.get("/api/auth/user") {
            header("Authorization", "Bearer $token")
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<Map<String, JsonElement>>(response.bodyAsText())
        assertEquals(userId, body["id"]?.jsonPrimitive?.content)
        assertEquals("test@example.com", body["email"]?.jsonPrimitive?.content)
    }
    
    @Test
    fun testUpdateUser() = testApplication {
        application {
            testModule()
        }
        
        val authService = AuthService()
        
        // Register and get token
        val registerRequest = RegisterRequest(
            email = "test@example.com",
            password = "TestPassword123!",
            name = "Test User"
        )
        
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(RegisterRequest.serializer(), registerRequest))
        }
        
        val registerBody = Json.decodeFromString<Map<String, JsonElement>>(registerResponse.bodyAsText())
        val token = registerBody["token"]?.jsonPrimitive?.content ?: fail("Token not found")
        
        // Update user
        val updateRequest = mapOf("name" to "Updated Name")
        
        val response = client.put("/api/auth/user") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(JsonObject.serializer(), JsonObject(updateRequest.mapValues { JsonPrimitive(it.value) })))
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<Map<String, JsonElement>>(response.bodyAsText())
        assertEquals("Updated Name", body["name"]?.jsonPrimitive?.content)
    }
}
