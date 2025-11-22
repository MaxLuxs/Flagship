<p align="center">
  <img src="images/flagship_icon.svg" width="120" height="120" alt="Flagship Logo">
</p>

<h1 align="center">📚 API Reference</h1>

Complete reference for the Flagship library public API.

---

## Core API

### `Flags` (Singleton)

Global access point to Flagship.

```kotlin
object Flags {
    fun configure(config: FlagsConfig)
    fun manager(): FlagsManager
}
```

#### `configure(config: FlagsConfig)`

Initializes Flagship with the given configuration. Call **once** at app startup.

**Parameters:**
- `config: FlagsConfig` - library configuration

**Throws:**
- `IllegalStateException` - if `configure()` was already called

**Example:**
```kotlin
Flags.configure(
    FlagsConfig(
        appKey = "my-app",
        environment = "production",
        providers = listOf(FirebaseRemoteConfigProvider(Firebase.remoteConfig)),
        cache = InMemoryCache(),
        logger = DefaultLogger()
    )
)
```

#### `manager(): FlagsManager`

Returns a `FlagsManager` instance for working with flags.

**Returns:** `FlagsManager`

**Throws:**
- `IllegalStateException` - if `configure()` was not called

**Example:**
```kotlin
val manager = Flags.manager()
```

---

### `FlagsManager` (Interface)

Main interface for working with flags and experiments.

```kotlin
interface FlagsManager {
    // Feature Flags (suspend functions)
    suspend fun isEnabled(key: FlagKey, default: Boolean = false, ctx: EvalContext? = null): Boolean
    suspend fun <T> value(key: FlagKey, default: T, ctx: EvalContext? = null): T
    
    // Experiments (suspend function)
    suspend fun assign(key: ExperimentKey, ctx: EvalContext? = null): ExperimentAssignment?
    
    // Lifecycle
    suspend fun bootstrap(): Boolean
    suspend fun refresh(): Boolean
    suspend fun ensureBootstrap(timeoutMs: Long): Boolean
    
    // Overrides (Debug only)
    fun setOverride(key: FlagKey, value: FlagValue)
    fun clearOverride(key: FlagKey)
    suspend fun listOverrides(): Map<FlagKey, FlagValue>
    
    // Introspection (suspend function)
    suspend fun listAllFlags(): Map<FlagKey, FlagValue>
    
    // Listeners
    fun addListener(listener: FlagsListener)
    fun removeListener(listener: FlagsListener)
}
```

#### `isEnabled(key, default, ctx): Boolean`

Checks if a feature flag is enabled.

**Parameters:**
- `key: String` - flag key
- `default: Boolean = false` - default value (if flag not found)
- `ctx: EvalContext? = null` - context for targeting (optional)

**Returns:** `Boolean` - `true` if flag is enabled, otherwise `false`

**Example:**
```kotlin
// Note: isEnabled is a suspend function - use in coroutine scope
lifecycleScope.launch {
    if (manager.isEnabled("dark_mode")) {
        enableDarkTheme()
    }
}
```

#### `value<T>(key, default, ctx): T`

Gets a typed flag value.

**Type Parameters:**
- `T` - value type (String, Int, Double, Boolean, JsonElement)

**Parameters:**
- `key: String` - flag key
- `default: T` - default value
- `ctx: EvalContext? = null` - context for targeting

**Returns:** `T` - flag value or `default` if not found/wrong type

**Example:**
```kotlin
// Note: value is a suspend function - use in coroutine scope
lifecycleScope.launch {
    val timeout: Int = manager.value("request_timeout", default = 5000)
    val apiUrl: String = manager.value("api_base_url", default = "https://api.example.com")
}
```

#### `assign(key, ctx): ExperimentAssignment?`

Assigns a user to an experiment variant.

**Parameters:**
- `key: String` - experiment key
- `ctx: EvalContext? = null` - user context (userId required!)

**Returns:** `ExperimentAssignment?` - assignment or `null` if experiment not found

**Example:**
```kotlin
// Note: assign is a suspend function - use in coroutine scope
lifecycleScope.launch {
    val assignment = manager.assign(
        "checkout_test",
        EvalContext(userId = "user_123")
    )

    when (assignment?.variant) {
        "control" -> OldCheckout()
        "variant_a" -> NewCheckout()
    }
}
```

#### `bootstrap(): Boolean`

Asynchronously loads data from all providers.

**Returns:** `Boolean` - `true` if loading successful

**Example:**
```kotlin
lifecycleScope.launch {
    val success = manager.bootstrap()
    if (success) {
        // Data loaded
    }
}
```

#### `refresh(): Boolean`

Forces data refresh from all providers (ignoring TTL).

**Returns:** `Boolean` - `true` if refresh successful

**Example:**
```kotlin
lifecycleScope.launch {
    manager.refresh()
}
```

#### `ensureBootstrap(timeoutMs): Boolean`

Ensures data is loaded with timeout.

**Parameters:**
- `timeoutMs: Long` - timeout in milliseconds

**Returns:** `Boolean` - `true` if data loaded within timeout

**Example:**
```kotlin
val success = manager.ensureBootstrap(5000) // 5 seconds
```

#### `setOverride(key, value)`

Sets a local flag override (for debugging).

**Parameters:**
- `key: String` - flag key
- `value: FlagValue` - new value

**Example:**
```kotlin
manager.setOverride("dark_mode", FlagValue.Bool(true))
```

#### `clearOverride(key)`

Removes a local override.

**Parameters:**
- `key: String` - flag key

**Example:**
```kotlin
manager.clearOverride("dark_mode")
```

#### `listOverrides(): List<String>`

Returns list of all active overrides.

**Returns:** `List<String>` - keys of flags with overrides

#### `listAllFlags(): Map<String, FlagValue>`

Returns all available flags and their values.

**Returns:** `Map<String, FlagValue>`

#### `addListener(listener)`

Adds a change listener.

**Parameters:**
- `listener: FlagsListener`

**Example:**
```kotlin
manager.addListener(object : FlagsListener {
    override fun onSnapshotUpdated(providersCount: Int) {
        // Update UI
    }
    
    override fun onOverrideChanged(key: String) {
        // Override changed
    }
})
```

#### `removeListener(listener)`

Removes a listener.

**Parameters:**
- `listener: FlagsListener`

---

### `FlagsConfig` (Data Class)

Flagship configuration.

```kotlin
data class FlagsConfig(
    val appKey: String,
    val environment: String,
    val providers: List<FlagsProvider>,
    val cache: FlagsCache = InMemoryCache(),
    val logger: FlagsLogger = DefaultLogger(),
    val analytics: AnalyticsAdapter? = null
)
```

**Fields:**
- `appKey: String` - unique application identifier
- `environment: String` - environment ("production", "staging", "dev")
- `providers: List<FlagsProvider>` - list of providers (in priority order)
- `cache: FlagsCache` - cache for offline support
- `logger: FlagsLogger` - logger for debugging
- `analytics: AnalyticsAdapter?` - analytics adapter (optional)

---

### `EvalContext` (Data Class)

User context for targeting.

```kotlin
data class EvalContext(
    val userId: String,
    val attributes: Map<String, Any> = emptyMap()
)
```

**Fields:**
- `userId: String` - unique user ID (required for experiments)
- `attributes: Map<String, Any>` - additional attributes for targeting

**Common attributes:**
- `region: String` - country code ("US", "GB", "RU")
- `app_version: String` - app version ("2.5.0")
- `os_version: String` - OS version ("14.5")
- `device_type: String` - device type ("phone", "tablet")
- `subscription_tier: String` - subscription level ("free", "premium")

**Example:**
```kotlin
val ctx = EvalContext(
    userId = "user_12345",
    attributes = mapOf(
        "region" to "US",
        "app_version" to "2.6.0",
        "subscription_tier" to "premium"
    )
)
```

---

### `FlagValue` (Sealed Class)

Typed flag value.

```kotlin
sealed class FlagValue {
    data class Bool(val value: Boolean) : FlagValue()
    data class String(val value: kotlin.String) : FlagValue()
    data class Int(val value: kotlin.Int) : FlagValue()
    data class Double(val value: kotlin.Double) : FlagValue()
    data class Json(val value: JsonElement) : FlagValue()
}
```

**Example:**
```kotlin
val flag1 = FlagValue.Bool(true)
val flag2 = FlagValue.String("hello")
val flag3 = FlagValue.Int(42)
val flag4 = FlagValue.Double(3.14)
val flag5 = FlagValue.Json(buildJsonObject { put("key", "value") })
```

---

### `ExperimentAssignment` (Data Class)

Experiment assignment result.

```kotlin
data class ExperimentAssignment(
    val experimentKey: String,
    val variant: String,
    val payload: JsonObject = JsonObject(emptyMap()),
    val assignmentHash: String
)
```

**Fields:**
- `experimentKey: String` - experiment key
- `variant: String` - variant name ("control", "variant_a", etc.)
- `payload: JsonObject` - additional variant data
- `assignmentHash: String` - assignment hash (for debugging)

**Example:**
```kotlin
val assignment = manager.assign("button_color_test")
println("Variant: ${assignment?.variant}") // "variant_a"
val color = assignment?.payload["color"]?.jsonPrimitive?.content // "#FF5733"
```

---

### `FlagsListener` (Interface)

Flags change listener.

```kotlin
interface FlagsListener {
    fun onSnapshotUpdated(providersCount: Int)
    fun onOverrideChanged(key: FlagKey)
}
```

#### `onSnapshotUpdated(providersCount)`

Called when snapshot is updated from providers.

**Parameters:**
- `providersCount: Int` - number of successfully updated providers

#### `onOverrideChanged(key)`

Called when local override changes.

**Parameters:**
- `key: String` - key of changed flag

---

## Providers

### `FlagsProvider` (Interface)

Interface for custom providers.

```kotlin
interface FlagsProvider {
    val name: String
    suspend fun fetch(context: EvalContext): ProviderSnapshot
}
```

### `FirebaseRemoteConfigProvider`

```kotlin
class FirebaseRemoteConfigProvider(
    private val remoteConfig: FirebaseRemoteConfig,
    private val fetchIntervalSeconds: Long = 3600
) : FlagsProvider
```

### `RestFlagsProvider`

```kotlin
class RestFlagsProvider(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : FlagsProvider
```

---

## Cache

### `FlagsCache` (Interface)

```kotlin
interface FlagsCache {
    suspend fun save(providerName: String, snapshot: ProviderSnapshot)
    suspend fun load(providerName: String): ProviderSnapshot?
    suspend fun clear(providerName: String)
    suspend fun clearAll()
}
```

### `InMemoryCache`

Stores data in memory (not persistent).

```kotlin
class InMemoryCache : FlagsCache
```

### `PersistentCache`

Stores data on disk (Android: SharedPreferences, iOS: UserDefaults).

```kotlin
class PersistentCache(
    private val serializer: FlagsSerializer
) : FlagsCache
```

---

## Analytics

### `AnalyticsAdapter` (Interface)

```kotlin
interface AnalyticsAdapter {
    fun trackEvent(event: AnalyticsEvent)
}
```

### `AnalyticsEvent` (Sealed Class)

```kotlin
sealed class AnalyticsEvent {
    data class ExperimentExposure(
        val experimentKey: String,
        val variant: String,
        val timestamp: Long
    ) : AnalyticsEvent()
}
```

---

## Compose UI

### `FlagsDashboard`

```kotlin
@Composable
fun FlagsDashboard(
    manager: FlagsManager,
    allowOverrides: Boolean = true,
    allowEnvSwitch: Boolean = false,
    useDarkTheme: Boolean = false
)
```

**Parameters:**
- `manager: FlagsManager` - FlagsManager instance
- `allowOverrides: Boolean` - allow local overrides
- `allowEnvSwitch: Boolean` - show environment switcher
- `useDarkTheme: Boolean` - use dark theme

**Example:**
```kotlin
@Composable
fun DebugScreen() {
    FlagsDashboard(
        manager = Flags.manager(),
        allowOverrides = true,
        allowEnvSwitch = false
    )
}
```

---

<p align="center">
  <b>Complete API documentation: <a href="https://maxluxs.github.io/Flagship/">Dokka HTML</a></b>
</p>

