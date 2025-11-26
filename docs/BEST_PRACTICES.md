# Best Practices

## Configuration

### Provider Order
Order providers by priority (first has highest priority):
```kotlin
val providers = listOf(
    RestFlagsProvider(...),      // Primary source
    FirebaseRemoteConfigProvider(...), // Fallback
    FileFlagsProvider(...)        // Local override
)
```

### Caching Strategy
- Use `PersistentCache` for production (survives app restarts)
- Use `LruCache` for memory-constrained environments
- Set appropriate TTL values

### Error Handling

Flagship provides multiple ways to handle errors when working with flags. Choose the approach that best fits your use case.

#### Default-Based Error Handling (Recommended for Most Cases)

The simplest approach is to always provide sensible defaults. Flagship will return the default value if:
- The flag doesn't exist
- A provider fails
- There's a type mismatch
- Cache is unavailable

**Example:**
```kotlin
// Simple and safe - always returns a value
val timeout = flags.value("api_timeout", default = 5000)
if (flags.isEnabled("new_feature", default = false)) {
    // Safe default - false if anything goes wrong
}
```

**When to use:**
- Most common use cases
- When you don't need to know why a flag failed
- When defaults are acceptable fallbacks

#### Result-Based API (For Explicit Error Handling)

When you need to know if an error occurred or want to handle errors explicitly, use the Result-based API.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = flags.valueOrError("api_timeout", default = 5000)
    result.onSuccess { timeout ->
        println("Got timeout: $timeout")
        configureApi(timeout)
    }.onFailure { error ->
        log.error("Failed to get flag", error)
        // Use fallback or show error to user
        showErrorToUser()
    }
}

// Or using getOrElse
val timeout = flags.valueOrError("api_timeout", default = 5000)
    .getOrElse { error ->
        log.error("Flag error", error)
        5000 // Fallback value
    }
```

**When to use:**
- When you need to log errors
- When you want to show errors to users
- When you need different behavior based on error type
- In critical paths where errors must be handled

#### FlagStatus API (For Debugging and Monitoring)

Use `getFlagStatus()` to get detailed information about a flag's state, including where the value came from and any errors.

**Example:**
```kotlin
lifecycleScope.launch {
    val status = flags.getFlagStatus("new_feature")
    
    when (status.source) {
        FlagSource.OVERRIDE -> log.debug("Using override value")
        FlagSource.PROVIDER -> log.debug("Using fresh provider value")
        FlagSource.CACHE -> log.warn("Using cached value, may be stale")
        FlagSource.DEFAULT -> log.warn("Flag not found, using default")
    }
    
    if (status.lastError != null) {
        log.error("Error getting flag", status.lastError)
    }
    
    if (!status.isHealthy()) {
        // Flag is in an unhealthy state
        alertMonitoringSystem()
    }
}
```

**When to use:**
- Debugging flag issues
- Monitoring flag health
- Understanding flag evaluation flow
- Building admin dashboards

#### Best Practices

1. **Always provide defaults** - Even when using Result-based API
   ```kotlin
   // Good
   val result = flags.valueOrError("key", default = 100)
   
   // Avoid
   val result = flags.valueOrError("key", default = null) // No default!
   ```

2. **Log errors appropriately** - Don't ignore failures
   ```kotlin
   flags.valueOrError("critical_flag", default = false)
       .onFailure { error ->
           log.error("Critical flag failed", error)
           // Take appropriate action
       }
   ```

3. **Use FlagStatus for monitoring** - Track flag health
   ```kotlin
   // In monitoring code
   val status = flags.getFlagStatus("important_flag")
   if (!status.isHealthy()) {
       metrics.recordFlagError("important_flag")
   }
   ```

4. **Handle errors gracefully** - Don't crash on flag errors
   ```kotlin
   val enabled = flags.isEnabledOrError("feature", default = false)
       .getOrElse { error ->
           log.warn("Flag check failed, using default", error)
           false // Safe fallback
       }
   ```

## Bootstrap

### Initialization
Always bootstrap before using flags:
```kotlin
lifecycleScope.launch {
    val success = flags.ensureBootstrap(timeoutMs = 3000)
    if (!success) {
        // Handle timeout - use cached/default values
    }
}
```

### Background Refresh
Refresh periodically:
```kotlin
// On app foreground
lifecycleScope.launch {
    flags.refresh(force = false) { providerName, success ->
        if (!success) {
            logger.warn("Refresh failed for $providerName")
        }
    }
}
```

## Flag Usage

### Type Safety
Use typed accessors:
```kotlin
// Good
val timeout: Int = flags.value("timeout", default = 5000)

// Avoid
val timeout = flags.value("timeout", default = "5000") // String!
```

### Context
Always provide evaluation context for experiments:
```kotlin
val context = EvalContext(
    userId = currentUser.id,
    deviceId = deviceId,
    region = userRegion,
    attributes = mapOf("tier" to userTier)
)
val assignment = flags.assign("exp_key", context)
```

### Overrides
Use overrides only for testing/debugging:
```kotlin
// Development only
if (BuildConfig.DEBUG) {
    flags.setOverride("new_feature", FlagValue.Bool(true))
}
```

## Performance

### When to Use Sync vs Async

Flagship provides both synchronous and asynchronous methods for accessing flags. Understanding when to use each is crucial for optimal performance.

#### Async Methods (Default)

**Use async methods when:**
- Bootstrap hasn't completed yet
- You're in a coroutine scope (lifecycleScope, viewModelScope, etc.)
- You need to wait for fresh data from providers
- You're in a suspend function

**Example:**
```kotlin
// In a coroutine scope
lifecycleScope.launch {
    val enabled = flags.isEnabled("new_feature")
    if (enabled) {
        showNewFeature()
    }
}

// In a suspend function
suspend fun checkFeature(): Boolean {
    return flags.isEnabled("new_feature", default = false)
}
```

**Benefits:**
- Works before bootstrap completes
- Can trigger provider refresh if needed
- Thread-safe and non-blocking

#### Sync Methods

**Use sync methods when:**
- Bootstrap has already completed (after `ensureBootstrap()`)
- You're in a non-suspending context (Compose, callbacks, etc.)
- You need maximum performance (no coroutine overhead)
- You're accessing flags frequently (e.g., in a loop)

**Example:**
```kotlin
// After bootstrap
lifecycleScope.launch {
    flags.ensureBootstrap(timeoutMs = 3000)
    
    // Now safe to use sync methods
    if (flags.isEnabledSync("new_feature")) {
        showNewFeature()
    }
}

// In Compose (after bootstrap)
@Composable
fun FeatureScreen() {
    val enabled = remember {
        flags.isEnabledSync("new_feature", default = false)
    }
    
    if (enabled) {
        NewFeatureContent()
    }
}
```

**Benefits:**
- Zero coroutine overhead
- Immediate return (no suspension)
- Can be used in non-suspending contexts

**Important:** Sync methods will throw `IllegalStateException` if called before bootstrap completes. Always ensure bootstrap is done first.

#### Performance Recommendations

1. **Initial Load:** Use async methods during app startup
   ```kotlin
   lifecycleScope.launch {
       flags.ensureBootstrap()
       // Now ready for sync methods
   }
   ```

2. **Frequent Access:** Use sync methods after bootstrap
   ```kotlin
   // In a loop or frequent checks
   for (item in items) {
       if (flags.isEnabledSync("feature_for_item")) {
           processItem(item)
       }
   }
   ```

3. **UI Rendering:** Use sync methods in Compose after bootstrap
   ```kotlin
   @Composable
   fun MyScreen() {
       val timeout = flags.intValueSync("api_timeout", default = 5000)
       // Use timeout immediately
   }
   ```

4. **Background Operations:** Use async methods for background tasks
   ```kotlin
   viewModelScope.launch {
       val config = flags.value("background_config", default = emptyMap())
       // Process config
   }
   ```

### Sync Methods
Use sync methods after bootstrap for better performance:
```kotlin
// After ensureBootstrap()
if (flags.isEnabledSync("feature")) {
    // Fast, no coroutine overhead
}
```

### Caching
- Configure appropriate cache size
- Monitor cache hit rates
- Clear expired entries periodically

### Provider Health
Monitor provider health:
```kotlin
providers.forEach { provider ->
    if (!provider.isHealthy()) {
        // Handle unhealthy provider
    }
}
```

## Testing

### Unit Tests
Use test providers:
```kotlin
val testProvider = TestProvider()
val config = FlagsConfig(
    providers = listOf(testProvider),
    cache = InMemoryCache()
)
```

### Integration Tests
Test with real providers:
```kotlin
val restProvider = RestFlagsProvider(client, baseUrl)
// Test with real API
```

## Security

### API Keys
Never commit API keys to version control:
```kotlin
val apiKey = System.getenv("FLAGSHIP_API_KEY")
    ?: throw IllegalStateException("API key not found")
```

### Signature Verification
Enable signature verification for production:
```kotlin
val config = FlagsConfig(
    // ...
    crypto = Crypto(publicKey)
)
```

## Monitoring

### Metrics
Track provider metrics:
```kotlin
val metrics = metricsTracker.getMetrics(providerName)
if (metrics.successRate < 0.95) {
    // Alert on low success rate
}
```

### Logging
Use appropriate log levels:
```kotlin
val logger = DefaultLogger(level = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO)
```

