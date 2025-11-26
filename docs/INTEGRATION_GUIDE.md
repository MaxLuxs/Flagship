# 📘 Flagship Integration Guide

Detailed step-by-step guide for integrating the Flagship library into your project.

---

## 📋 Table of Contents

1. [Requirements](#-requirements)
2. [Dependency Installation](#-dependency-installation)
3. [Initialization](#-initialization)
4. [Usage in Code](#-usage-in-code)
5. [Provider Configuration](#-provider-configuration)
6. [Platform Examples](#-platform-examples)
7. [Common Scenarios](#-common-scenarios)
8. [Troubleshooting](#-troubleshooting)

---

## ✅ Requirements

### Minimum Versions

- **Kotlin**: 1.9.0+
- **Android**: API 21+ (Android 5.0+)
- **iOS**: 13.0+
- **Gradle**: 7.0+
- **Kotlin Multiplatform**: 1.9.0+

### For Firebase Provider

- **Firebase SDK**: 20.0.0+
- **Google Services Plugin**: 4.3.0+

### For REST Provider

- **Ktor Client**: 2.0.0+

---

## 📦 Dependency Installation

### Step 1: Configure Repositories

In the root `build.gradle.kts` (or `settings.gradle.kts`), add:

```kotlin
repositories {
    mavenCentral()
    google() // For Firebase
}
```

### Step 2: Add Dependencies

#### Option A: Android Project (Kotlin/JVM)

In `app/build.gradle.kts`:

```kotlin
dependencies {
    // Required: core library
    implementation("io.maxluxs.flagship:flagship-core:0.1.1")
    
    // Optional: Firebase provider
    implementation("io.maxluxs.flagship:flagship-provider-firebase:0.1.1")
    
    // Optional: REST provider
    implementation("io.maxluxs.flagship:flagship-provider-rest:0.1.1")
    
    // Optional: Debug UI (development only)
    debugImplementation("io.maxluxs.flagship:flagship-admin-ui-compose:0.1.1")
    
    // Ktor Client required for REST provider
    implementation("io.ktor:ktor-client-android:2.3.5")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
}
```

#### Option B: Kotlin Multiplatform Project

In `commonMain` source set:

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Required: core library
                implementation("io.maxluxs.flagship:flagship-core:0.1.1")
                
                // Optional: REST provider (works on all platforms)
                implementation("io.maxluxs.flagship:flagship-provider-rest:0.1.1")
                
                // Optional: Debug UI
                implementation("io.maxluxs.flagship:flagship-admin-ui-compose:0.1.1")
            }
        }
        
        val androidMain by getting {
            dependencies {
                // Firebase provider only for Android
                implementation("io.maxluxs.flagship:flagship-provider-firebase:0.1.1")
                
                // Ktor for Android
                implementation("io.ktor:ktor-client-android:2.3.5")
            }
        }
        
        val iosMain by getting {
            dependencies {
                // Ktor for iOS
                implementation("io.ktor:ktor-client-darwin:2.3.5")
            }
        }
    }
}
```

#### Option C: iOS Project (Swift Package Manager)

1. In Xcode: File → Add Packages...
2. Add repository URL (or local path to XCFramework)
3. Select modules:
   - `FlagshipCore`
   - `FlagshipProviderRest` (optional)

---

## 🚀 Initialization

### Android Project

#### Step 1: Create Application Class

```kotlin
package com.example.myapp

import android.app.Application
import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.platform.AndroidFlagsInitializer
import io.maxluxs.flagship.provider.rest.RestFlagsProvider
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeFlagship()
    }
    
    private fun initializeFlagship() {
        // Option 1: Simplified initialization with Firebase
        // import io.maxluxs.flagship.provider.firebase.initFirebase
        // Flagship.initFirebase(
        //     application = this,
        //     defaults = mapOf(
        //         "new_feature" to false,
        //         "max_retries" to 3
        //     )
        // )
        
        // Option 2: Full configuration with REST provider
        val httpClient = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
        
        val restProvider = RestFlagsProvider(
            client = httpClient,
            baseUrl = "https://api.example.com/flags" // Your API endpoint
        )
        
        val config = FlagsConfig(
            appKey = "my-app",
            environment = "production", // or "development", "staging"
            providers = listOf(restProvider),
            cache = AndroidFlagsInitializer.createPersistentCache(this)
        )
        
        Flagship.configure(config)
        
        // Set default context (optional, but recommended)
        val manager = Flagship.manager() as io.maxluxs.flagship.core.manager.DefaultFlagsManager
        val defaultContext = AndroidFlagsInitializer.createDefaultContext(this)
        manager.setDefaultContext(defaultContext)
    }
}
```

#### Step 2: Register Application in AndroidManifest.xml

```xml
<manifest ...>
    <application
        android:name=".MyApplication"
        ...>
        ...
    </application>
</manifest>
```

### Kotlin Multiplatform Project

#### Common Initialization (commonMain)

```kotlin
// commonMain/kotlin/AppInitializer.kt
package com.example.shared

import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.cache.InMemoryCache

expect fun createHttpClient(): HttpClient
expect fun createPersistentCache(): FlagsCache

fun initializeFlagship() {
    val httpClient = createHttpClient()
    val restProvider = RestFlagsProvider(
        client = httpClient,
        baseUrl = "https://api.example.com/flags"
    )
    
    val config = FlagsConfig(
        appKey = "my-kmp-app",
        environment = "production",
        providers = listOf(restProvider),
        cache = createPersistentCache()
    )
    
    Flagship.configure(config)
}
```

#### Android Implementation (androidMain)

```kotlin
// androidMain/kotlin/AppInitializer.android.kt
package com.example.shared

import io.maxluxs.flagship.core.cache.FlagsCache
import io.maxluxs.flagship.core.platform.AndroidFlagsInitializer
import io.ktor.client.*
import io.ktor.client.engine.android.*
import android.content.Context

actual fun createHttpClient(): HttpClient {
    return HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}

actual fun createPersistentCache(): FlagsCache {
    // Context needed - pass it via parameter or singleton
    return AndroidFlagsInitializer.createPersistentCache(context)
}
```

#### iOS Implementation (iosMain)

```kotlin
// iosMain/kotlin/AppInitializer.ios.kt
package com.example.shared

import io.maxluxs.flagship.core.cache.FlagsCache
import io.maxluxs.flagship.core.platform.IOSFlagsInitializer
import io.ktor.client.*
import io.ktor.client.engine.darwin.*

actual fun createHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}

actual fun createPersistentCache(): FlagsCache {
    return IOSFlagsInitializer.createPersistentCache()
}
```

---

## 💻 Usage in Code

### ⚠️ Important: All flag access methods are suspend functions!

All methods `isEnabled()`, `value()`, `assign()` are suspend functions and must be called in a coroutine scope.

### Feature Flags

```kotlin
import androidx.lifecycle.lifecycleScope
import io.maxluxs.flagship.core.Flagship
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            // Check boolean flag
            if (Flagship.isEnabled("new_feature")) {
                showNewFeature()
            } else {
                showOldFeature()
            }
            
            // Or using manager
            val manager = Flagship.manager()
            if (manager.isEnabled("dark_mode")) {
                enableDarkTheme()
            }
        }
    }
}
```

### Remote Configuration

```kotlin
lifecycleScope.launch {
    val manager = Flagship.manager()
    
    // Get typed values
    val maxRetries: Int = manager.value("max_retries", default = 3)
    val apiTimeout: Double = manager.value("api_timeout", default = 30.0)
    val welcomeMessage: String = manager.value("welcome_msg", default = "Hello!")
    
    // Use values
    configureApi(maxRetries, apiTimeout)
    showWelcomeMessage(welcomeMessage)
}
```

### A/B Testing (Experiments)

```kotlin
import io.maxluxs.flagship.core.model.EvalContext

lifecycleScope.launch {
    val manager = Flagship.manager()
    
    // Simple assignment (without context)
    val assignment = manager.assign("checkout_experiment")
    when (assignment?.variant) {
        "control" -> showLegacyCheckout()
        "variant_a" -> showNewCheckout()
        else -> showLegacyCheckout() // Fallback
    }
    
    // With user context (recommended for consistency)
    val context = EvalContext(
        userId = "user_12345", // ⚠️ Important for consistent distribution
        appVersion = "2.5.0",
        osName = "Android",
        osVersion = "12",
        region = "US",
        attributes = mapOf("tier" to "premium")
    )
    
    val experiment = manager.assign("premium_ui", context = context)
    when (experiment?.variant) {
        "control" -> showStandardUI()
        "premium" -> showPremiumUI()
        else -> showStandardUI()
    }
}
```

### Using in Compose

```kotlin
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun MyScreen() {
    var isNewFeatureEnabled by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isNewFeatureEnabled = Flagship.isEnabled("new_feature")
    }
    
    if (isNewFeatureEnabled) {
        NewFeatureContent()
    } else {
        OldFeatureContent()
    }
}
```

### Using in ViewModel

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.maxluxs.flagship.core.Flagship
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyViewModel : ViewModel() {
    private val _featureEnabled = MutableStateFlow(false)
    val featureEnabled: StateFlow<Boolean> = _featureEnabled
    
    init {
        viewModelScope.launch {
            _featureEnabled.value = Flagship.isEnabled("new_feature")
        }
    }
}
```

---

## 🔌 Provider Configuration

### Firebase Remote Config

#### Step 1: Add Firebase to Project

Follow the [official Firebase setup guide](https://firebase.google.com/docs/android/setup).

#### Step 2: Initialization

```kotlin
import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.provider.firebase.initFirebase // ⚠️ Important: import extension function

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase (if not done already)
        Firebase.initializeApp(this)
        
        // Initialize Flagship with Firebase
        Flagship.initFirebase(
            application = this,
            defaults = mapOf(
                "new_feature" to false,
                "max_retries" to 3,
                "welcome_message" to "Hello!"
            )
        )
    }
}
```

### REST API Provider

#### Step 1: Configure HTTP Client

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val httpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        })
    }
}
```

#### Step 2: Create Provider

```kotlin
import io.maxluxs.flagship.provider.rest.RestFlagsProvider

val restProvider = RestFlagsProvider(
    client = httpClient,
    baseUrl = "https://api.example.com/flags",
    // Optional: custom headers
    headers = mapOf(
        "Authorization" to "Bearer YOUR_TOKEN",
        "X-App-Version" to "2.5.0"
    )
)
```

#### Step 3: API Response Format

Your REST API should return JSON in the following format:

```json
{
  "revision": "abc123",
  "fetchedAt": 1699999999999,
  "ttlMs": 900000,
  "flags": {
    "new_feature": { "type": "bool", "value": true },
    "max_retries": { "type": "int", "value": 5 },
    "api_timeout": { "type": "double", "value": 30.0 },
    "welcome_msg": { "type": "string", "value": "Hello!" }
  },
  "experiments": {
    "checkout_exp": {
      "variants": [
        { "name": "control", "weight": 0.5 },
        { "name": "variant_a", "weight": 0.5 }
      ],
      "targeting": {
        "type": "region_in",
        "regions": ["US", "CA"]
      }
    }
  }
}
```

---

## 📱 Platform Examples

### Android (Kotlin)

```kotlin
// Application.kt
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Flagship.initFirebase(this, defaults = mapOf("feature" to false))
    }
}

// Activity.kt
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            if (Flagship.isEnabled("new_ui")) {
                setContentView(R.layout.activity_new)
            } else {
                setContentView(R.layout.activity_old)
            }
        }
    }
}
```

### iOS (Swift)

```swift
import Flagship

@main
struct MyApp: App {
    init() {
        // Initialize via Kotlin/Native framework
        FlagshipSwift.shared.configure(...)
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

// ContentView.swift
struct ContentView: View {
    @State private var featureEnabled = false
    
    var body: some View {
        VStack {
            if featureEnabled {
                NewFeatureView()
            } else {
                OldFeatureView()
            }
        }
        .task {
            featureEnabled = await FlagshipSwift.shared.manager.isEnabled(
                key: "new_feature",
                default: false
            )
        }
    }
}
```

### Compose Multiplatform

```kotlin
@Composable
fun App() {
    var featureEnabled by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        featureEnabled = Flagship.isEnabled("new_feature")
    }
    
    if (featureEnabled) {
        NewFeatureScreen()
    } else {
        OldFeatureScreen()
    }
}
```

---

## 🎯 Common Scenarios

### Scenario 1: Gradual Rollout of New Feature

```kotlin
lifecycleScope.launch {
    val manager = Flagship.manager()
    
    // Flag can be enabled for 10% of users
    if (manager.isEnabled("new_payment_flow")) {
        showNewPaymentFlow()
    } else {
        showLegacyPaymentFlow()
    }
}
```

### Scenario 2: A/B Test UI

```kotlin
lifecycleScope.launch {
    val assignment = Flagship.assign("ui_experiment")
    
    when (assignment?.variant) {
        "control" -> {
            // Old UI
            setContentView(R.layout.activity_old_ui)
        }
        "variant_a" -> {
            // New UI variant A
            setContentView(R.layout.activity_new_ui_a)
        }
        "variant_b" -> {
            // New UI variant B
            setContentView(R.layout.activity_new_ui_b)
        }
        else -> {
            // Fallback
            setContentView(R.layout.activity_old_ui)
        }
    }
}
```

### Scenario 3: Dynamic Configuration

```kotlin
class ApiClient {
    private var maxRetries = 3
    private var timeout = 30.0
    
    init {
        // Update configuration when flags change
        Flagship.manager().addListener(object : FlagsListener {
            override fun onSnapshotUpdated(source: String) {
                lifecycleScope.launch {
                    maxRetries = Flagship.manager().value("max_retries", 3)
                    timeout = Flagship.manager().value("api_timeout", 30.0)
                    updateHttpClient(maxRetries, timeout)
                }
            }
        })
    }
}
```

### Scenario 4: Kill Switch (Emergency Disable)

```kotlin
lifecycleScope.launch {
    // If flag is disabled, show error instead of function
    if (!Flagship.isEnabled("payment_system")) {
        showError("Payment system temporarily unavailable")
        return@launch
    }
    
    // Continue work
    processPayment()
}
```

---

## 🔧 Troubleshooting

### Problem 1: "Unresolved reference 'initFirebase'"

**Solution**: Add import for extension function:

```kotlin
import io.maxluxs.flagship.provider.firebase.initFirebase
```

### Problem 2: "Suspend function should be called only from coroutine"

**Solution**: Use coroutine scope:

```kotlin
// ❌ Incorrect
if (Flagship.isEnabled("feature")) { ... }

// ✅ Correct
lifecycleScope.launch {
    if (Flagship.isEnabled("feature")) { ... }
}
```

### Problem 3: Flags Not Loading

**Check**:
1. Is provider configured correctly?
2. Is API endpoint accessible?
3. Is API response format correct?
4. Is there internet connection?
5. Check logs: `Flagship.manager().refresh()` and look for errors

### Problem 4: Flags Not Updating

**Solution**: 
- Flags update automatically every 15 minutes
- For forced update: `Flagship.manager().refresh(force = true)`
- Check TTL in API response

### Problem 5: Experiments Give Different Variants for Same User

**Solution**: Always pass `userId` in `EvalContext`:

```kotlin
val context = EvalContext(
    userId = "user_12345", // ⚠️ Required!
    appVersion = "2.5.0",
    osName = "Android",
    osVersion = "12"
)
val assignment = Flagship.assign("experiment", context = context)
```

---

## 📚 Additional Resources

- [Complete Usage Guide](USAGE_GUIDE.md)
- [API Reference](API_REFERENCE.md)
- [Simplified API](SIMPLIFIED_API.md)
- [Code Examples](../samples/)

---

## 💡 Tips

1. **Always use default values** - this ensures app works even if flags fail to load
2. **Pass userId in experiments** - for consistent distribution
3. **Use Debug UI in development** - for testing flags without changing remote config
4. **Don't block UI** - all methods are suspend, use coroutine scope
5. **Test offline mode** - library works with cache when network is unavailable

---

**Done!** Now you can use Flagship in your project. If you have questions, check the [FAQ](USAGE_GUIDE.md#faq) or create an issue in the repository.
