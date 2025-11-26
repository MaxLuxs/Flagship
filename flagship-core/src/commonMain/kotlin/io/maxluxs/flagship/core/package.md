# flagship-core

Core module of Flagship containing the primary API, evaluation engine, and cache system.

## What's Inside

### Public API
- `Flagship` - Global singleton for library configuration and access
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
- `PersistentCache` - Platform-specific persistent cache

