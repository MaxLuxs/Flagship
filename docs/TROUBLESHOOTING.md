# Troubleshooting Guide

## Common Issues and Solutions

### Flags not loading

**Problem:** Flags return default values even after bootstrap.

**Solutions:**
1. Check that `Flagship.configure()` or `Flagship.init()` was called before using flags
2. Ensure `ensureBootstrap()` completed successfully:
   ```kotlin
   lifecycleScope.launch {
       val success = flags.ensureBootstrap(timeoutMs = 5000)
       if (!success) {
           // Bootstrap timed out - check network/providers
       }
   }
   ```
3. Check provider health:
   ```kotlin
   providers.forEach { provider ->
       if (!provider.isHealthy()) {
           logger.warn("Provider ${provider.name} is unhealthy")
       }
   }
   ```
4. Check cache - expired data might be used:
   ```kotlin
   // Clear cache and retry
   cache.clearAll()
   flags.refresh(force = true)
   ```

### Network errors

**Problem:** REST provider fails with network errors.

**Solutions:**
1. Check network connectivity
2. Verify base URL is correct
3. Check API key/authentication
4. Review circuit breaker state:
   ```kotlin
   // Circuit breaker might be open after failures
   // Wait for timeout or reset manually
   ```
5. Check retry policy configuration
6. Review timeout settings

### Type mismatches

**Problem:** Getting wrong type when reading flag value.

**Solutions:**
1. Verify flag type in backend matches expected type
2. Use typed accessors:
   ```kotlin
   val value: Int = flags.value("timeout", default = 5000)
   ```
3. Check flag value in debugger:
   ```kotlin
   val allFlags = flags.listAllFlags()
   println(allFlags["timeout"]) // Check actual type
   ```

### Experiments not assigning

**Problem:** `assign()` returns null for experiments.

**Solutions:**
1. Check targeting rules - user might not qualify
2. Verify experiment exists in provider
3. Check evaluation context:
   ```kotlin
   val context = EvalContext(
       userId = "user123",
       deviceId = "device456",
       region = "US"
   )
   val assignment = flags.assign("exp_key", context)
   ```
4. Use debugger to diagnose:
   ```kotlin
   val diagnostics = debugger.diagnoseExperiment("exp_key", context)
   println(diagnostics.availableInProviders)
   ```

### Cache issues

**Problem:** Stale data or cache not working.

**Solutions:**
1. Check TTL settings - data might be expired
2. Clear cache:
   ```kotlin
   cache.clear(providerName)
   // or
   cache.clearAll()
   ```
3. Force refresh:
   ```kotlin
   flags.refresh(force = true)
   ```
4. Check cache implementation - InMemoryCache vs PersistentCache

### Provider errors

**Problem:** Provider fails to fetch data.

**Solutions:**
1. Check provider health:
   ```kotlin
   if (!provider.isHealthy()) {
       val failures = provider.getConsecutiveFailures()
       val lastSuccess = provider.getLastSuccessfulFetchMs()
       // Investigate based on metrics
   }
   ```
2. Review provider-specific logs
3. Check provider configuration
4. Verify provider is reachable (for REST/Firebase)

### Performance issues

**Problem:** Slow flag evaluation or bootstrap.

**Solutions:**
1. Use sync methods after bootstrap:
   ```kotlin
   // After ensureBootstrap()
   if (flags.isEnabledSync("feature")) {
       // Fast, no suspend
   }
   ```
2. Enable lazy loading for non-critical providers
3. Optimize cache size (LruCache with maxSize)
4. Review provider metrics:
   ```kotlin
   val metrics = metricsTracker.getMetrics(providerName)
   println("Avg response time: ${metrics.averageResponseTimeMs}ms")
   ```

### Sync method errors

**Problem:** `IllegalStateException` when calling sync methods.

**Solutions:**
1. Ensure bootstrap completes before using sync methods:
   ```kotlin
   lifecycleScope.launch {
       val success = flags.ensureBootstrap(timeoutMs = 3000)
       if (success) {
           // Now safe to use sync methods
           val enabled = flags.isEnabledSync("feature")
       }
   }
   ```
2. Use async methods if bootstrap hasn't completed:
   ```kotlin
   // Use async method instead
   lifecycleScope.launch {
       val enabled = flags.isEnabled("feature")
   }
   ```

### Error handling issues

**Problem:** Need to know when flags fail or handle errors explicitly.

**Solutions:**
1. Use Result-based API for explicit error handling:
   ```kotlin
   lifecycleScope.launch {
       val result = Flagship.valueOrError("critical_flag", default = false)
       result.onSuccess { value ->
           useValue(value)
       }.onFailure { error ->
           log.error("Flag failed", error)
           handleError()
       }
   }
   ```
2. Use FlagStatus for detailed information:
   ```kotlin
   lifecycleScope.launch {
       val status = Flagship.getFlagStatus("flag_key")
       if (!status.isHealthy()) {
           // Flag is in unhealthy state
           alertMonitoring()
       }
   }
   ```
3. Always provide sensible defaults even with Result API

## Debug Mode

Enable verbose logging:

```kotlin
val config = FlagsConfig(
    // ...
    logger = DefaultLogger(level = LogLevel.DEBUG)
)
```

Use FlagsDebugger for diagnostics:

```kotlin
val debugger = FlagsDebugger(config, manager, providers, metricsTracker, logger)
val debugInfo = debugger.getDebugInfo()
println(debugInfo)
```

## Getting Help

1. Check logs for error messages
2. Use FlagsDebugger to get diagnostic information
3. Review provider metrics
4. Check network connectivity and API endpoints
5. Verify configuration is correct

