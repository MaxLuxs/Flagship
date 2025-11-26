package io.maxluxs.flagship.server.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationUtilsTest {
    
    @Test
    fun testValidateEmail() {
        assertTrue(ValidationUtils.validateEmail("test@example.com"))
        assertTrue(ValidationUtils.validateEmail("user.name+tag@example.co.uk"))
        assertFalse(ValidationUtils.validateEmail("invalid"))
        assertFalse(ValidationUtils.validateEmail("invalid@"))
        assertFalse(ValidationUtils.validateEmail("@example.com"))
        assertFalse(ValidationUtils.validateEmail(""))
    }
    
    @Test
    fun testValidatePassword() {
        val result1 = ValidationUtils.validatePassword("Password123")
        assertTrue(result1.isValid)
        
        val result2 = ValidationUtils.validatePassword("short")
        assertFalse(result2.isValid)
        
        val result3 = ValidationUtils.validatePassword("nouppercase123")
        assertFalse(result3.isValid)
        
        val result4 = ValidationUtils.validatePassword("NOLOWERCASE123")
        assertFalse(result4.isValid)
        
        val result5 = ValidationUtils.validatePassword("NoDigitsHere")
        assertFalse(result5.isValid)
    }
    
    @Test
    fun testValidateSlug() {
        assertTrue(ValidationUtils.validateSlug("my-project"))
        assertTrue(ValidationUtils.validateSlug("project123"))
        assertFalse(ValidationUtils.validateSlug("MyProject"))
        assertFalse(ValidationUtils.validateSlug("my_project"))
        assertFalse(ValidationUtils.validateSlug("-project"))
        assertFalse(ValidationUtils.validateSlug("project-"))
    }
    
    @Test
    fun testValidateFlagKey() {
        assertTrue(ValidationUtils.validateFlagKey("my_flag"))
        assertTrue(ValidationUtils.validateFlagKey("flag123"))
        assertTrue(ValidationUtils.validateFlagKey("FLAG-NAME"))
        assertFalse(ValidationUtils.validateFlagKey(""))
        assertFalse(ValidationUtils.validateFlagKey("flag with spaces"))
    }
    
    @Test
    fun testValidateUUID() {
        assertTrue(ValidationUtils.validateUUID("550e8400-e29b-41d4-a716-446655440000"))
        assertFalse(ValidationUtils.validateUUID("invalid-uuid"))
        assertFalse(ValidationUtils.validateUUID(""))
        assertFalse(ValidationUtils.validateUUID("550e8400-e29b-41d4"))
    }
}

