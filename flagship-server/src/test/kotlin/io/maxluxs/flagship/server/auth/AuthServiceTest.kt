package io.maxluxs.flagship.server.auth

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthServiceTest {
    
    @Test
    fun testHashPassword() {
        val service = AuthService()
        val password = "TestPassword123"
        val hash = service.hashPassword(password)
        
        assertNotNull(hash)
        assertTrue(hash.length > 10)
        assertTrue(service.verifyPassword(password, hash))
    }
    
    @Test
    fun testVerifyPassword() {
        val service = AuthService()
        val password = "TestPassword123"
        val hash = service.hashPassword(password)
        
        assertTrue(service.verifyPassword(password, hash))
        assertTrue(!service.verifyPassword("wrong", hash))
    }
    
    @Test
    fun testGenerateToken() {
        val service = AuthService()
        val userId = java.util.UUID.randomUUID()
        val token = service.generateToken(userId, "test@example.com", false)
        
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }
    
    @Test
    fun testVerifyToken() {
        val service = AuthService()
        val userId = java.util.UUID.randomUUID()
        val token = service.generateToken(userId, "test@example.com", false)
        
        val payload = service.verifyToken(token)
        assertNotNull(payload)
        assertTrue(payload.userId == userId)
        assertTrue(payload.email == "test@example.com")
        assertTrue(payload.isAdmin == false)
    }
    
    @Test
    fun testVerifyInvalidToken() {
        val service = AuthService()
        val payload = service.verifyToken("invalid.token.here")
        
        assertNull(payload)
    }
}

