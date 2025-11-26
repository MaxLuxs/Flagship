package io.maxluxs.flagship.shared.api

import kotlinx.serialization.Serializable

/**
 * Shared API request/response models for Admin Panel and Server.
 * 
 * These models are used for:
 * - Server API endpoints (flagship-server)
 * - Admin UI API client (flagship-admin-ui-compose)
 */

// Authentication models
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val name: String?,
    val isAdmin: Boolean
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null
)

// Project models
@Serializable
data class CreateProjectRequest(
    val name: String,
    val slug: String,
    val description: String? = null
)

@Serializable
data class UpdateProjectRequest(
    val name: String? = null,
    val description: String? = null
)

@Serializable
data class ProjectResponse(
    val id: String,
    val name: String,
    val slug: String,
    val description: String?,
    val ownerId: String,
    val createdAt: Long,
    val updatedAt: Long
)

// API Key models
@Serializable
data class CreateApiKeyRequest(
    val name: String,
    val type: String, // "READ_ONLY" or "ADMIN"
    val expirationDays: Int? = null // Optional expiration in days
)

@Serializable
data class ApiKeyResponse(
    val id: String,
    val name: String,
    val key: String, // Only shown once on creation
    val type: String,
    val createdAt: Long,
    val expiresAt: Long? = null
)

// Extended flag request with metadata
@Serializable
data class CreateFlagRequest(
    val flag: RestFlagValue,
    val description: String? = null,
    val isEnabled: Boolean = true
)

@Serializable
data class UpdateFlagRequest(
    val flag: RestFlagValue,
    val description: String? = null,
    val isEnabled: Boolean? = null
)

// Flag response with metadata
@Serializable
data class FlagResponse(
    val key: String,
    val type: String,
    val value: String,
    val description: String?,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

// Experiment models
/**
 * Request model for creating a new experiment.
 * 
 * @property experiment Experiment configuration (variants, targeting, etc.)
 * @property description Optional description of the experiment
 * @property isActive Whether the experiment is active (default: true)
 */
@Serializable
data class CreateExperimentRequest(
    val experiment: RestExperiment,
    val description: String? = null,
    val isActive: Boolean = true
)

/**
 * Request model for updating an existing experiment.
 * 
 * @property experiment Updated experiment configuration
 * @property description Optional updated description
 * @property isActive Optional updated active status
 */
@Serializable
data class UpdateExperimentRequest(
    val experiment: RestExperiment,
    val description: String? = null,
    val isActive: Boolean? = null
)

/**
 * Response model for experiment with metadata.
 * 
 * @property key Experiment key/identifier
 * @property experiment Experiment configuration
 * @property description Experiment description
 * @property isActive Whether the experiment is currently active
 * @property createdAt Timestamp when the experiment was created (milliseconds since epoch)
 * @property updatedAt Timestamp when the experiment was last updated (milliseconds since epoch)
 */
@Serializable
data class ExperimentResponse(
    val key: String,
    val experiment: RestExperiment,
    val description: String?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

// Project member models
/**
 * Response model for a project member.
 * 
 * @property userId ID of the user
 * @property email Email address of the user
 * @property name Display name of the user (optional)
 * @property role Role in the project (OWNER, ADMIN, MEMBER, VIEWER)
 * @property joinedAt Timestamp when the user joined the project (milliseconds since epoch)
 */
@Serializable
data class ProjectMemberResponse(
    val userId: String,
    val email: String,
    val name: String?,
    val role: String, // OWNER, ADMIN, MEMBER, VIEWER
    val joinedAt: Long
)

/**
 * Request model for adding a member to a project.
 * 
 * @property email Email address of the user to add
 * @property role Role to assign (ADMIN, MEMBER, VIEWER)
 */
@Serializable
data class AddProjectMemberRequest(
    val email: String,
    val role: String // ADMIN, MEMBER, VIEWER
)

// Audit log models
/**
 * Audit log entry representing a single action in the system.
 * 
 * @property id Unique identifier of the audit log entry
 * @property action Type of action performed (e.g., FLAG_CREATED, EXPERIMENT_UPDATED)
 * @property entityType Type of entity affected (e.g., "flag", "experiment", "project")
 * @property entityId ID of the affected entity (optional)
 * @property projectId ID of the project (optional)
 * @property userId ID of the user who performed the action (optional)
 * @property changes Map of changes made (optional)
 * @property ipAddress IP address of the client (optional)
 * @property userAgent User agent string of the client (optional)
 * @property createdAt Timestamp when the action was performed (seconds since epoch)
 */
@Serializable
data class AuditLogEntry(
    val id: String,
    val action: String,
    val entityType: String,
    val entityId: String?,
    val projectId: String?,
    val userId: String?,
    val changes: Map<String, String>? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val createdAt: Long
)

