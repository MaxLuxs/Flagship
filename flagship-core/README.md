<p align="center">
  <img src="../docs/images/flagship_icon.svg" width="120" height="120" alt="Flagship Logo">
</p>

# flagship-core

Core module of Flagship containing the primary API, evaluation engine, and cache system.

## 📦 What's Inside

### Public API
- `Flags` - Global singleton for library configuration and access
- `FlagsManager` - Main interface for flag/experiment evaluation
- `FlagsConfig` - Configuration builder

### Models
- `FlagValue` - Type-safe flag values (Bool, String, Int, Double, Json)
- `ExperimentAssignment` - A/B test assignment result
- `ProviderSnapshot` - Immutable snapshot of flags/experiments from a provider
- `EvalContext` - User context for targeting (userId, attributes, etc.)
- `TargetingRule` - Targeting conditions (Region, AppVersion, etc.)

### Evaluation Engine
- `FlagsEvaluator` - Evaluates flags and experiments
- `TargetingEvaluator` - Evaluates targeting rules
- `BucketingEngine` - Deterministic bucketing using MurmurHash3

### Cache
- `FlagsCache` - Interface for caching snapshots
- `InMemoryCache` - Thread-safe in-memory implementation
- `PersistentCache` - Platform-specific disk cache (SharedPreferences on Android, UserDefaults on iOS)

### Concurrency
- All operations are thread-safe using `Mutex`
- Suspending functions for async operations
- `Dispatchers.IO` for disk operations on Android

## 🚀 Usage

```kotlin
// Initialize
val config = FlagsConfig(
    appKey = "my-app",
    environment = "production",
    providers = listOf(/* your providers */),
    cache = InMemoryCache(),
    logger = DefaultLogger()
)
Flags.configure(config)

// Get manager
val manager = Flags.manager()

// Feature flag
if (manager.isEnabled("new_checkout")) {
    // Show new checkout
}

// Experiment
val assignment = manager.assign("checkout_experiment")
when (assignment?.variant) {
    "control" -> // Original
    "variant_a" -> // Test A
    "variant_b" -> // Test B
}

// Type-safe values
val timeout: Int = manager.value("request_timeout", default = 5000)
val apiUrl: String = manager.value("api_base_url", default = "https://api.example.com")
```

## 🧪 Testing

This module includes comprehensive unit tests:
- `BucketingEngineTest` - Deterministic bucketing and distribution
- `FlagsManagerTypeSafetyTest` - Type safety and fallback behavior
- `TargetingEvaluatorTest` - Targeting rules and SemVer comparison

Run tests:
```bash
./gradlew :flagship-core:allTests
```

## 📚 Documentation

Full API documentation: [https://maxluxs.github.io/Flagship/](https://maxluxs.github.io/Flagship/)
