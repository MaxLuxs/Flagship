package io.maxluxs.flagship.shared.api

import kotlinx.serialization.Serializable

/**
 * Standard error response model for API errors.
 * 
 * Used across:
 * - Server API error responses (flagship-server)
 * - Admin UI error handling (flagship-admin-ui-compose)
 */
@Serializable
data class ErrorResponse(
    val error: String,
    val code: Int,
    val details: String? = null,
    val field: String? = null
) {
    companion object {
        /**
         * Creates a Bad Request (400) error response.
         */
        fun badRequest(message: String, field: String? = null): ErrorResponse {
            return ErrorResponse(
                error = "Bad Request",
                code = 400,
                details = message,
                field = field
            )
        }
        
        /**
         * Creates an Unauthorized (401) error response.
         */
        fun unauthorized(message: String = "Unauthorized"): ErrorResponse {
            return ErrorResponse(
                error = "Unauthorized",
                code = 401,
                details = message
            )
        }
        
        /**
         * Creates a Forbidden (403) error response.
         */
        fun forbidden(message: String = "Access denied"): ErrorResponse {
            return ErrorResponse(
                error = "Forbidden",
                code = 403,
                details = message
            )
        }
        
        /**
         * Creates a Not Found (404) error response.
         */
        fun notFound(resource: String = "Resource"): ErrorResponse {
            return ErrorResponse(
                error = "Not Found",
                code = 404,
                details = "$resource not found"
            )
        }
        
        /**
         * Creates a Conflict (409) error response.
         */
        fun conflict(message: String): ErrorResponse {
            return ErrorResponse(
                error = "Conflict",
                code = 409,
                details = message
            )
        }
        
        /**
         * Creates an Internal Server Error (500) error response.
         */
        fun internalError(message: String = "Internal server error"): ErrorResponse {
            return ErrorResponse(
                error = "Internal Server Error",
                code = 500,
                details = message
            )
        }
    }
}
