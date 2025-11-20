<p align="center">
  <img src="images/flagship_icon.svg" width="150" height="150" alt="Flagship Logo">
</p>

<h1 align="center">📖 Flagship Usage Guide</h1>

Complete guide for using the Flagship library in your Android and iOS applications.

---

## 📋 Table of Contents

1. [Quick Start](#-quick-start)
2. [Installation](#-installation)
3. [Initialization](#-initialization)
4. [Feature Flags](#-feature-flags)
5. [A/B Testing (Experiments)](#-ab-testing-experiments)
6. [Targeting Rules](#-targeting-rules)
7. [Providers](#-providers)
8. [Caching & Offline Mode](#-caching--offline-mode)
9. [Debug Dashboard](#-debug-dashboard)
10. [Analytics Integration](#-analytics-integration)
11. [Best Practices](#-best-practices)
12. [FAQ](#-faq)

---

## 🚀 Quick Start

### Android (Kotlin)

```kotlin
// 1. In Application class
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Recommended: Use factory for easy setup
        val config = FlagsConfig(
            appKey = "my-banking-app",
            environment = "production",
            providers = listOf(
                FirebaseProviderFactory.create(application)
            ),
            cache = PersistentCache(FlagsSerializer()),
            logger = DefaultLogger()
        )
        
        // Alternative: Manual setup (for advanced use cases)
        // val config = FlagsConfig(
        //     appKey = "my-banking-app",
        //     environment = "production",
        //     providers = listOf(
        //         FirebaseRemoteConfigProvider(AndroidFirebaseAdapter(Firebase.remoteConfig))
        //     ),
        //     cache = PersistentCache(FlagsSerializer()),
        //     logger = DefaultLogger()
        // )
        
        Flags.configure(config)
    }
}

// 2. In your app code
val flags = Flags.manager()

// Feature flag
if (flags.isEnabled("new_payment_flow")) {
    NewPaymentScreen()
} else {
    LegacyPaymentScreen()
}

// A/B experiment
val assignment = flags.assign("checkout_experiment")
when (assignment?.variant) {
    "control" -> OriginalCheckout()
    "variant_a" -> NewCheckoutA()
    "variant_b" -> NewCheckoutB()
}
```

### iOS (Swift)

```swift
// 1. In AppDelegate or App.swift
import Flagship

@main
struct MyApp: App {
    init() {
        FlagshipIOSInitializer.shared.initialize()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

// 2. In SwiftUI View
import Flagship

struct PaymentView: View {
    let flags = Flags.shared.manager()
    
    var body: some View {
        if flags.isEnabled(key: "new_payment_flow", default: false, ctx: nil) {
            NewPaymentView()
        } else {
            LegacyPaymentView()
        }
    }
}
```

---

## 📦 Installation

### Android

1. Add modules to `settings.gradle.kts`:
```kotlin
include(":flagship-core")
include(":flagship-provider-firebase")
include(":flagship-ui-compose") // optional
```

2. In `build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":flagship-core"))
    implementation(project(":flagship-provider-firebase"))
    implementation(project(":flagship-ui-compose")) // for debug dashboard
}
```

### iOS (Swift Package Manager)

1. In Xcode: **File > Add Packages...**
2. URL: `https://github.com/maxluxs/Flagship`
3. Version: `0.1.0`
4. Add Package

Or in `Package.swift`:
```swift
dependencies: [
    .package(url: "https://github.com/maxluxs/Flagship", from: "0.1.0")
]
```

---

## ⚙️ Initialization

### Basic Configuration

```kotlin
val config = FlagsConfig(
    appKey = "my-app",           // Unique app ID
    environment = "production",   // production, staging, dev
    providers = listOf(/* ... */),
    cache = InMemoryCache(),     // or PersistentCache()
    logger = DefaultLogger()
)

Flags.configure(config)
```

### With Multiple Providers

```kotlin
// Using factories (recommended)
val config = FlagsConfig(
    appKey = "my-app",
    environment = "production",
    providers = listOf(
        FirebaseProviderFactory.create(application), // Priority 1
        RestFlagsProvider(httpClient, "https://api.example.com/flags") // Fallback
    ),
    cache = PersistentCache(FlagsSerializer()),
    logger = DefaultLogger()
)
```

> **Note**: `FirebaseProviderFactory` handles Firebase initialization automatically. For manual setup, see the [Firebase Provider section](#firebase-remote-config).

**Important:** Providers are processed in order. The first successful result is used.

### Async Loading

```kotlin
// In a coroutine
lifecycleScope.launch {
    val manager = Flags.manager()
    
    // Attempt to load data (with 5 second timeout)
    val success = manager.ensureBootstrap(timeoutMs = 5000)
    
    if (success) {
        // Data loaded, can use flags
    } else {
        // Using cache or default values
    }
}
```

---

## 🎯 Feature Flags

### Simple Boolean Flag

```kotlin
val manager = Flags.manager()

if (manager.isEnabled("dark_mode")) {
    // Enable dark theme
}
```

### Typed Values

```kotlin
// String
val apiUrl: String = manager.value("api_base_url", default = "https://api.example.com")

// Int
val timeout: Int = manager.value("request_timeout", default = 5000)

// Double
val discount: Double = manager.value("promo_discount", default = 0.1)

// JSON
val config: JsonObject = manager.value("feature_config", default = buildJsonObject {})
```

### Type Safety

```kotlin
// If type doesn't match, default value is returned
val value: String = manager.value("some_int_flag", default = "fallback") // "fallback"
```

### Reactive Updates in Compose

```kotlin
@Composable
fun FeatureScreen() {
    val manager = Flags.manager()
    var featureEnabled by remember { mutableStateOf(manager.isEnabled("new_feature")) }
    
    // Listen for changes
    DisposableEffect(Unit) {
        val listener = object : FlagsListener {
            override fun onSnapshotUpdated(providersCount: Int) {
                featureEnabled = manager.isEnabled("new_feature")
            }
        }
        manager.addListener(listener)
        onDispose { manager.removeListener(listener) }
    }
    
    if (featureEnabled) {
        NewFeatureUI()
    } else {
        OldFeatureUI()
    }
}
```

---

## 🧪 A/B Testing (Experiments)

### Basic Usage

```kotlin
val assignment = manager.assign(
    key = "checkout_experiment",
    ctx = EvalContext(
        userId = "user_12345",
        attributes = mapOf("tier" to "premium")
    )
)

when (assignment?.variant) {
    "control" -> {
        // Original version (50%)
        OriginalCheckout()
    }
    "variant_a" -> {
        // Variant A (25%)
        CheckoutVariantA()
    }
    "variant_b" -> {
        // Variant B (25%)
        CheckoutVariantB()
    }
    else -> {
        // Fallback (if experiment is disabled)
        OriginalCheckout()
    }
}
```

### With Payload Data

```kotlin
val assignment = manager.assign("button_color_test")

assignment?.let {
    val buttonColor = it.payload["color"]?.jsonPrimitive?.content ?: "#007AFF"
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(parseColor(buttonColor))
        )
    ) { Text("Buy Now") }
}
```

### Deterministic Bucketing

Flagship uses **MurmurHash3** for deterministic distribution:
- Same `userId` always falls into the same group
- Uniform distribution (50/50, 33/33/34, etc.)
- Cross-platform consistency (Android ↔ iOS)

```kotlin
// Same user always gets the same variant
val user1 = manager.assign("exp", EvalContext(userId = "user_123"))
val user2 = manager.assign("exp", EvalContext(userId = "user_123"))
// user1.variant == user2.variant ✅
```

### Exposure Tracking

Flagship automatically sends exposure events for A/B tests:

```kotlin
val config = FlagsConfig(
    // ...
    analytics = object : AnalyticsAdapter {
        override fun trackEvent(event: AnalyticsEvent) {
            when (event) {
                is AnalyticsEvent.ExperimentExposure -> {
                    // Send to Firebase Analytics, Amplitude, etc.
                    firebaseAnalytics.logEvent("experiment_exposure") {
                        param("experiment_key", event.experimentKey)
                        param("variant", event.variant)
                    }
                }
            }
        }
    }
)
```

---

## 🎯 Targeting Rules

### Region-based Targeting

```kotlin
// Server returns experiment with targeting:
// "targeting": {
//   "type": "region_in",
//   "regions": ["US", "CA", "GB"]
// }

val assignment = manager.assign(
    "premium_feature_test",
    ctx = EvalContext(
        userId = "user_123",
        attributes = mapOf("region" to "US") // User from US
    )
)
// assignment != null ✅ (if user is from US, CA or GB)
```

### App Version Targeting

```kotlin
// "targeting": {
//   "type": "app_version_gte",
//   "version": "2.5.0"
// }

val assignment = manager.assign(
    "new_ui_rollout",
    ctx = EvalContext(
        userId = "user_123",
        attributes = mapOf("app_version" to "2.6.1")
    )
)
// assignment != null ✅ (2.6.1 >= 2.5.0)
```

Supports **Semantic Versioning** (SemVer):
- `1.0.0` > `1.0.0-beta`
- `2.1.0` > `2.0.9`
- `1.0.0-rc.2` > `1.0.0-beta.1`

### Custom Attributes

```kotlin
// "targeting": {
//   "type": "attribute_equals",
//   "key": "subscription_tier",
//   "value": "premium"
// }

val assignment = manager.assign(
    "premium_only_feature",
    ctx = EvalContext(
        userId = "user_123",
        attributes = mapOf(
            "subscription_tier" to "premium",
            "account_age_days" to 365
        )
    )
)
// assignment != null ✅ (if subscription_tier == "premium")
```

### Composite Rules (AND/OR)

```kotlin
// "targeting": {
//   "type": "composite",
//   "operator": "AND",
//   "rules": [
//     { "type": "region_in", "regions": ["US"] },
//     { "type": "app_version_gte", "version": "3.0.0" }
//   ]
// }

// User must be from US AND have version >= 3.0.0
```

---

## 🔌 Providers

### Firebase Remote Config

```kotlin
// build.gradle.kts
implementation(project(":flagship-provider-firebase"))

// Recommended: Using factory (Android)
import io.maxluxs.flagship.provider.firebase.FirebaseProviderFactory

val provider = FirebaseProviderFactory.create(
    application = application,
    defaults = mapOf(
        "new_feature" to false,
        "dark_mode" to false
    ),
    name = "firebase"
)

val config = FlagsConfig(
    appKey = "my-app",
    environment = "production",
    providers = listOf(provider),
    cache = PersistentCache(FlagsSerializer())
)
```

**Alternative: Manual Setup**

```kotlin
// For advanced use cases or iOS
val adapter = AndroidFirebaseAdapter(Firebase.remoteConfig)
val provider = FirebaseRemoteConfigProvider(adapter, name = "firebase")
```

**Firebase Console structure:**
```json
{
  "flagship_flags": {
    "new_payment_flow": true,
    "dark_mode_enabled": false
  },
  "flagship_experiments": {
    "checkout_test": {
      "variants": [
        { "name": "control", "weight": 0.5 },
        { "name": "variant_a", "weight": 0.5 }
      ]
    }
  }
}
```

### REST Provider

```kotlin
// build.gradle.kts
implementation(project(":flagship-provider-rest"))

// Code
val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}

val provider = RestFlagsProvider(
    httpClient = httpClient,
    baseUrl = "https://api.example.com/flags"
)
```

**REST API format:**
```json
{
  "revision": "abc123",
  "fetchedAt": 1699999999999,
  "ttlMs": 900000,
  "flags": {
    "new_payment_flow": { "type": "bool", "value": true },
    "api_timeout": { "type": "int", "value": 5000 }
  },
  "experiments": {
    "checkout_test": {
      "variants": [
        { "name": "control", "weight": 0.5 },
        { "name": "treatment", "weight": 0.5 }
      ],
      "targeting": {
        "type": "region_in",
        "regions": ["US", "CA"]
      }
    }
  }
}
```

### Custom Provider

```kotlin
class MyCustomProvider : FlagsProvider {
    override val name: String = "MyBackend"
    
    override suspend fun fetch(context: EvalContext): ProviderSnapshot {
        // Load data from your backend
        val response = myApi.getFlags()
        
        return ProviderSnapshot(
            revision = response.version,
            fetchedAt = Clock.System.now().toEpochMilliseconds(),
            ttlMs = 3600_000, // 1 hour
            flags = mapOf(
                "feature_x" to FlagValue.Bool(response.featureXEnabled)
            ),
            experiments = emptyMap()
        )
    }
}
```

---

## 💾 Caching & Offline Mode

### In-Memory Cache (default)

```kotlin
val config = FlagsConfig(
    // ...
    cache = InMemoryCache() // Fast but not persistent
)
```

**Pros:** Very fast  
**Cons:** Data lost on app restart

### Persistent Cache

```kotlin
val config = FlagsConfig(
    // ...
    cache = PersistentCache(FlagsSerializer())
)
```

**Android:** Uses `SharedPreferences`  
**iOS:** Uses `NSUserDefaults`

**Pros:** Works offline, persists between launches  
**Cons:** Slightly slower

### TTL (Time-To-Live)

Each snapshot has a TTL. After expiration, Flagship will attempt to refresh:

```kotlin
// In provider
ProviderSnapshot(
    revision = "v123",
    fetchedAt = Clock.System.now().toEpochMilliseconds(),
    ttlMs = 1800_000, // 30 minutes
    flags = myFlags,
    experiments = myExperiments
)
```

### Offline-first Architecture

```kotlin
// 1. Flagship tries to load data from providers
manager.bootstrap()

// 2. If network unavailable, uses cache
// 3. If no cache, uses default values
val enabled = manager.isEnabled("feature", default = false)
```

---

## 🛠️ Debug Dashboard

### Enable in Android

```kotlin
// In debug build
@Composable
fun DebugMenu() {
    val manager = Flags.manager()
    
    FlagsDashboard(
        manager = manager,
        allowOverrides = true,   // Allow overrides
        allowEnvSwitch = false,  // Disable environment switching
        useDarkTheme = false
    )
}
```

### Dashboard Features

1. **All Flags List** - current values and types
2. **Overrides** - force enable/disable flags
3. **Experiment Assignments** - view assigned variants
4. **Provider Status** - loading status from each provider
5. **Cache Info** - when data was loaded, TTL

### Local Overrides

```kotlin
// Programmatically
manager.setOverride("new_feature", FlagValue.Bool(true))

// Check
manager.isEnabled("new_feature") // true (override)

// Remove override
manager.clearOverride("new_feature")

// Remove all overrides
manager.listOverrides().forEach { manager.clearOverride(it) }
```

**⚠️ Important:** Overrides are local only and don't affect other users!

---

## 📊 Analytics Integration

### Firebase Analytics

```kotlin
val config = FlagsConfig(
    // ...
    analytics = object : AnalyticsAdapter {
        override fun trackEvent(event: AnalyticsEvent) {
            when (event) {
                is AnalyticsEvent.ExperimentExposure -> {
                    Firebase.analytics.logEvent("experiment_exposure") {
                        param("experiment_key", event.experimentKey)
                        param("variant", event.variant)
                        param("timestamp", event.timestamp)
                    }
                }
            }
        }
    }
)
```

### Amplitude

```kotlin
analytics = object : AnalyticsAdapter {
    override fun trackEvent(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.ExperimentExposure -> {
                Amplitude.getInstance().logEvent(
                    "Experiment Exposure",
                    JSONObject().apply {
                        put("experiment_key", event.experimentKey)
                        put("variant", event.variant)
                    }
                )
            }
        }
    }
}
```

### Mixpanel

```kotlin
analytics = object : AnalyticsAdapter {
    override fun trackEvent(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.ExperimentExposure -> {
                mixpanel.track(
                    "Experiment Exposure",
                    mapOf(
                        "experiment_key" to event.experimentKey,
                        "variant" to event.variant
                    )
                )
            }
        }
    }
}
```

---

## ✅ Best Practices

### 1. Use Meaningful Flag Names

```kotlin
// ✅ Good
"new_payment_flow"
"dark_mode_enabled"
"premium_features_unlock"

// ❌ Bad
"flag1"
"test"
"temp_feature"
```

### 2. Remove Old Flags

```kotlin
// After full rollout, remove the flag and simplify code:

// Before
if (manager.isEnabled("new_checkout")) {
    NewCheckout()
} else {
    OldCheckout()
}

// After (when new_checkout = 100%)
NewCheckout()
```

### 3. Always Provide Defaults

```kotlin
// ✅ Always provide defaults
val timeout = manager.value("request_timeout", default = 5000)

// ❌ Avoid nulls
val timeout = manager.value<Int?>("request_timeout", default = null) // Dangerous!
```

### 4. Cache Manager Instance

```kotlin
// ✅ Good
class MyRepository {
    private val flags = Flags.manager() // Once
    
    fun isFeatureEnabled() = flags.isEnabled("feature_x")
}

// ❌ Bad
fun checkFeature() {
    val flags = Flags.manager() // Creates new instance each time
    return flags.isEnabled("feature_x")
}
```

### 5. Test with Overrides

```kotlin
@Test
fun testNewPaymentFlow() {
    val manager = Flags.manager()
    
    // Force enable flag
    manager.setOverride("new_payment_flow", FlagValue.Bool(true))
    
    // Test
    assertTrue(manager.isEnabled("new_payment_flow"))
    
    // Cleanup
    manager.clearOverride("new_payment_flow")
}
```

### 6. Handle Errors

```kotlin
lifecycleScope.launch {
    try {
        val manager = Flags.manager()
        val success = manager.ensureBootstrap(5000)
        
        if (!success) {
            // Show warning or use fallback
            showOfflineMode()
        }
    } catch (e: Exception) {
        Log.e("Flags", "Failed to initialize", e)
        // Graceful degradation
    }
}
```

---

## ❓ FAQ

### Q: How does provider priority work?

**A:** Providers are processed in order in the list. The first successful result is used.

```kotlin
providers = listOf(
    FirebaseProvider(),  // Priority 1
    RestProvider(),      // Fallback 1
    LocalProvider()      // Fallback 2
)
```

### Q: What happens when offline?

**A:** Flagship uses the last cached snapshot. If no cache, uses default values.

### Q: Can I use Flagship without internet?

**A:** Yes! Use `PersistentCache()` and preload data once. Then the app works offline.

### Q: How often are flags updated?

**A:** Depends on TTL in snapshot (default 15 minutes). You can force refresh:

```kotlin
manager.refresh()
```

### Q: Is Hot Reload supported?

**A:** Yes! Use `FlagsListener` for reactive updates:

```kotlin
manager.addListener(object : FlagsListener {
    override fun onSnapshotUpdated(providersCount: Int) {
        // Update UI
    }
})
```

### Q: How to test experiments?

**A:** Use debug dashboard to override variants or use `setOverride()`.

### Q: Is local storage safe?

**A:** Yes. Flagship doesn't store sensitive data (tokens, keys). Only public flag settings.

### Q: Can I use Flagship in Compose Desktop/Web?

**A:** Currently only Android and iOS are supported. Desktop/Web may be added in the future.

---

<p align="center">
  <b>Need help? Create an Issue on <a href="https://github.com/maxluxs/Flagship/issues">GitHub</a>!</b>
</p>
