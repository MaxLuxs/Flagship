package io.maxluxs.flagship.provider.rest

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.maxluxs.flagship.core.circuit.CircuitBreaker
import io.maxluxs.flagship.core.circuit.CircuitBreakerOpenException
import io.maxluxs.flagship.core.errors.NetworkException
import io.maxluxs.flagship.core.errors.ParseException
import io.maxluxs.flagship.core.errors.ProviderException
import io.maxluxs.flagship.core.errors.ValidationException
import io.maxluxs.flagship.core.model.ProviderSnapshot
import io.maxluxs.flagship.core.provider.BaseFlagsProvider
import io.maxluxs.flagship.core.provider.ProviderErrorHandler
import io.maxluxs.flagship.core.provider.ProviderMetricsTracker
import io.maxluxs.flagship.core.retry.ExponentialBackoffRetry
import io.maxluxs.flagship.core.retry.RetryPolicy
import io.maxluxs.flagship.core.util.FlagsLogger
import io.maxluxs.flagship.core.util.NoopLogger
import io.maxluxs.flagship.core.util.SystemClock
import io.maxluxs.flagship.shared.api.RestResponse

/**
 * REST provider with retry logic, circuit breaker, and error handling.
 *
 * Features:
 * - Exponential backoff retry
 * - Circuit breaker for fault tolerance
 * - HTTP error handling (4xx, 5xx)
 * - Response validation
 * - Caching last successful response
 * - Configurable timeouts
 *
 * @property client HTTP client for requests
 * @property baseUrl Base URL for REST API
 * @property name Provider name (default: "rest")
 * @property retryPolicy Retry policy (default: ExponentialBackoffRetry with 3 attempts)
 * @property circuitBreaker Circuit breaker (default: 5 failures threshold, 60s timeout)
 * @property timeoutMs Request timeout in milliseconds (default: 10000ms = 10s)
 * @property logger Logger for debug messages
 * @property validateResponse Whether to validate response structure (default: true)
 */
class RestFlagsProvider(
    private val client: HttpClient,
    private val baseUrl: String,
    name: String = "rest",
    private val retryPolicy: RetryPolicy = ExponentialBackoffRetry(maxAttempts = 3),
    private val circuitBreaker: CircuitBreaker = CircuitBreaker(),
    private val timeoutMs: Long = 10_000,
    private val logger: FlagsLogger = NoopLogger,
    private val validateResponse: Boolean = true,
    metricsTracker: ProviderMetricsTracker? = null
) : BaseFlagsProvider(name, clock = SystemClock, metricsTracker = metricsTracker) {

    init {
        // Initialize error handler for common error handling
        errorHandler = ProviderErrorHandler(
            providerName = name,
            retryPolicy = retryPolicy,
            logger = logger,
            snapshotCache = snapshotCache
        )
    }

    public override suspend fun fetchSnapshot(currentRevision: String?): ProviderSnapshot {
        val url = if (currentRevision != null) {
            "$baseUrl/config?rev=$currentRevision"
        } else {
            "$baseUrl/config"
        }

        return try {
            circuitBreaker.execute {
                fetchWithErrorHandling(url)
            }
        } catch (e: CircuitBreakerOpenException) {
            logger.warn(name, "Circuit breaker is OPEN, using cached snapshot")
            snapshotCache.get() ?: throw ProviderException(
                name,
                "Circuit breaker is OPEN and no cached snapshot available",
                e
            )
        } catch (e: NetworkException) {
            logger.error(name, "Network error, using cached snapshot", e)
            snapshotCache.get() ?: throw e
        } catch (e: ParseException) {
            logger.error(name, "Parse error", e)
            snapshotCache.get() ?: throw e
        } catch (e: ValidationException) {
            logger.error(name, "Validation error", e)
            snapshotCache.get() ?: throw e
        } catch (e: Exception) {
            logger.error(name, "Unexpected error", e)
            snapshotCache.get() ?: throw ProviderException(
                name,
                "Unexpected error: ${e.message}",
                e
            )
        }
    }

    private suspend fun fetchWithErrorHandling(url: String): ProviderSnapshot {
        val response: HttpResponse = try {
            client.get(url)
        } catch (e: Exception) {
            when {
                e.message?.contains("timeout", ignoreCase = true) == true -> {
                    throw NetworkException("Request timeout after ${timeoutMs}ms: ${e.message}", e)
                }

                else -> {
                    throw NetworkException("Failed to connect to $url: ${e.message}", e)
                }
            }
        }

        // Handle HTTP errors
        when (response.status.value) {
            in 200..299 -> {
                // Success
            }

            in 400..499 -> {
                val errorBody = try {
                    response.bodyAsText()
                } catch (e: Exception) {
                    "Unable to read error body"
                }
                throw NetworkException(
                    "Client error ${response.status.value}: $errorBody"
                )
            }

            in 500..599 -> {
                throw NetworkException(
                    "Server error ${response.status.value}: ${response.status.description}"
                )
            }

            else -> {
                throw NetworkException(
                    "Unexpected status ${response.status.value}: ${response.status.description}"
                )
            }
        }

        // Parse response
        val restResponse: RestResponse = try {
            response.body()
        } catch (e: Exception) {
            throw ParseException("Failed to parse response: ${e.message}", e)
        }

        // Validate response
        if (validateResponse) {
            validateRestResponse(restResponse)
        }

        // Convert to snapshot
        val snapshot = try {
            restResponse.toProviderSnapshot()
        } catch (e: Exception) {
            throw ParseException("Failed to convert response to snapshot: ${e.message}", e)
        }

        // Cache successful snapshot (also cached in BaseFlagsProvider.bootstrap/refresh)
        snapshotCache.update(snapshot)

        logger.info(name, "Successfully fetched snapshot (revision: ${snapshot.revision})")
        return snapshot
    }

    private fun validateRestResponse(response: RestResponse) {
        // Validate flags
        response.flags.forEach { (key, flagValue) ->
            if (key.isBlank()) {
                throw ValidationException("Flag key cannot be blank")
            }

            val validTypes = setOf("bool", "int", "double", "string", "json")
            if (!validTypes.contains(flagValue.type)) {
                throw ValidationException(
                    "Invalid flag type '${flagValue.type}' for flag '$key'. " +
                            "Valid types: ${validTypes.joinToString()}"
                )
            }
        }

        // Validate experiments
        response.experiments.forEach { (key, experiment) ->
            if (key.isBlank()) {
                throw ValidationException("Experiment key cannot be blank")
            }

            if (experiment.variants.isEmpty()) {
                throw ValidationException("Experiment '$key' must have at least one variant")
            }

            val totalWeight = experiment.variants.sumOf { it.weight }
            if (totalWeight !in 0.99..1.01) {
                throw ValidationException(
                    "Experiment '$key' variant weights must sum to 1.0, got $totalWeight"
                )
            }

            experiment.variants.forEach { variant ->
                if (variant.name.isBlank()) {
                    throw ValidationException("Variant name cannot be blank in experiment '$key'")
                }
                if (variant.weight < 0 || variant.weight > 1) {
                    throw ValidationException(
                        "Variant '${variant.name}' weight must be between 0 and 1, got ${variant.weight}"
                    )
                }
            }
        }
    }
}

