<p align="center">
  <img src="docs/images/flagship_icon.svg" width="200" height="200" alt="Flagship Logo">
</p>

<h1 align="center">🚩 Flagship</h1>

<h3 align="center">
  Kotlin Multiplatform Feature Flags & A/B Testing Library
</h3>

<p align="center">
  <b>Flagship</b> is a modern, robust, and universal <b>Kotlin Multiplatform (KMP)</b> library for <b>Feature Flags</b>, <b>Feature Toggles</b>, <b>A/B Testing</b>, and <b>Remote Configuration</b>.
</p>

> ⚠️ **Note**: This library is currently in active development. The API may change in future versions. Use at your own risk.

<p align="center">
  It is designed to be <b>backend-agnostic</b>, supporting Firebase Remote Config, custom REST APIs, and local evaluations out of the box.
</p>

<p align="center">
  <i>Keywords: feature flags, feature toggles, feature switches, remote config, A/B testing, experiments, kotlin multiplatform, KMP, Android, iOS, gradual rollout, kill switch</i>
</p>

<p align="center">
  <a href="http://kotlinlang.org"><img src="https://img.shields.io/badge/kotlin-2.1.21-blue.svg?logo=kotlin" alt="Kotlin"></a>
  <a href="https://kotlinlang.org/docs/multiplatform.html"><img src="https://img.shields.io/badge/platform-android%20%7C%20ios-green.svg" alt="Platform"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License"></a>
  <a href="https://maxluxs.github.io/Flagship/"><img src="https://img.shields.io/badge/docs-latest-brightgreen.svg" alt="Documentation"></a>
</p>

---

## 🎯 Why Flagship?

Flagship is a **Kotlin Multiplatform feature flag library** that enables you to:

- 🔄 **Feature Toggles** - Enable/disable features remotely without app updates
- 🧪 **A/B Testing** - Run experiments and compare variants with statistical significance
- 📊 **Gradual Rollouts** - Deploy new features to a percentage of users
- 🛡️ **Kill Switches** - Instantly disable problematic features in production
- 🎯 **Remote Configuration** - Change app behavior and settings dynamically
- 🚀 **Kotlin Multiplatform** - Share logic between Android and iOS

Perfect for: mobile apps, KMP projects, gradual feature releases, experimentation, and runtime configuration.

---

## ✨ Features

- **🚀 Multiplatform**: Native support for **Android** and **iOS**.
- **🔌 Pluggable Providers**: Use Firebase Remote Config, REST APIs, or custom sources. Multiple providers with fallback strategies.
- **🧪 A/B Testing & Experiments**: Built-in experimentation engine with deterministic bucketing (MurmurHash3) and variant assignment.
- **🎯 Advanced Targeting**: Target users by Region, App Version (SemVer), OS, Language, or Custom Attributes.
- **🛡️ Safety First**: Offline-first architecture, automatic rollback to last good snapshot, and thread-safe concurrency.
- **📊 Analytics Ready**: Hooks for exposure tracking (assignment events) to integrate with Google Analytics, Amplitude, Mixpanel, or Segment.
- **📈 Provider Analytics** (🆕 **FREE**): Automatic monitoring of provider health, performance metrics (success rate, response time), and failure tracking. Built-in dashboard in admin panel.
- **🕵️ Debug Dashboard**: A drop-in **Compose Multiplatform** UI for inspecting flags, forcing overrides, and diagnostics.

---

## 📦 Installation

> 📘 **New**: Detailed integration guide is available in [INTEGRATION_GUIDE.md](docs/INTEGRATION_GUIDE.md)

Add the dependencies to your `build.gradle.kts`:

```kotlin
// Common Main
dependencies {
    implementation("io.maxluxs.flagship:flagship-core:0.1.1")
    implementation("io.maxluxs.flagship:flagship-provider-firebase:0.1.1") // Optional
    implementation("io.maxluxs.flagship:flagship-provider-rest:0.1.1")     // Optional
}

// Platform-specific code is included in flagship-core
// No separate platform modules needed!
```

---

## 🚀 Quick Start

### Method 1: Simplified API (Recommended for Beginners)

The simplest way to get started - just one line of initialization!

**Step 1: Add dependency**
```kotlin
dependencies {
    implementation("io.maxluxs.flagship:flagship-core:0.1.1")
    implementation("io.maxluxs.flagship:flagship-provider-firebase:0.1.1")
}
```

**Step 2: Initialize in Application**
```kotlin
import android.app.Application
import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.provider.firebase.initFirebase // ⚠️ Important: import extension function

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // One line initialization!
        Flagship.initFirebase(application)
    }
}
```

**Step 3: Use flags in your code**
```kotlin
import androidx.lifecycle.lifecycleScope
import io.maxluxs.flagship.core.Flagship
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ⚠️ Important: All flag methods are suspend functions - use in coroutine scope
        lifecycleScope.launch {
            // Check boolean flag
            if (Flagship.isEnabled("new_payment_flow")) {
                showNewPayment()
            } else {
                showLegacyPayment()
            }
            
            // Get typed values (type is inferred from default)
            val maxUploadSize: Int = Flagship.value("max_upload_mb", default = 10)
            val apiTimeout: Int = Flagship.value("api_timeout", default = 30)
            val welcomeMsg: String = Flagship.value("welcome_message", default = "Hello!")
        }
    }
}
```

> 📖 **See [Simplified API Guide](docs/SIMPLIFIED_API.md) for more details and examples.**

### Method 2: Full Configuration API

For advanced use cases requiring custom configuration, multiple providers, or custom analytics:

```kotlin
import android.app.Application
import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.core.manager.DefaultFlagsManager
import io.maxluxs.flagship.core.platform.AndroidFlagsInitializer
import io.maxluxs.flagship.core.util.DefaultLogger
import io.maxluxs.flagship.provider.firebase.FirebaseProviderFactory
import io.maxluxs.flagship.provider.rest.RestFlagsProvider
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeFlagship()
    }
    
    private fun initializeFlagship() {
        // Create HTTP client for REST provider (if using REST)
        val httpClient = HttpClient(Android) {
            install(ContentNegotiation) {
                json()
            }
        }
        
        // Configure Flagship
        val config = FlagsConfig(
            appKey = "my-app",
            environment = if (BuildConfig.DEBUG) "development" else "production",
            providers = listOf(
                // Primary provider: Firebase
                FirebaseProviderFactory.create(application),
                // Fallback provider: REST API
                RestFlagsProvider(httpClient, "https://api.myserver.com/flags")
            ),
            cache = AndroidFlagsInitializer.createPersistentCache(application),
            logger = DefaultLogger()
        )
        
        Flagship.configure(config)
        
        // Set default context (for targeting and experiments)
        val manager = Flagship.manager() as DefaultFlagsManager
        val defaultContext = AndroidFlagsInitializer.createDefaultContext(this)
        manager.setDefaultContext(defaultContext)
    }
}

// Use flags in your code
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val flags = Flagship.manager()
        
        // ⚠️ Important: All flag methods are suspend functions
        lifecycleScope.launch {
            if (flags.isEnabled("new_payment_flow")) {
                showNewPayment()
            }
        }
    }
}
```

### A/B Testing

Both methods support A/B testing with deterministic bucketing:

```kotlin
import io.maxluxs.flagship.core.model.EvalContext

lifecycleScope.launch {
    // Simplified API
    val variant = Flagship.assign("checkout_optimization_exp")?.variant
    
    // Or with Full API and custom context
    val assignment = Flagship.manager().assign(
        key = "checkout_optimization_exp",
        ctx = EvalContext(
            userId = "user_12345",
            appVersion = "2.5.0",
            osName = "Android",
            osVersion = "12",
            region = "US",
            attributes = mapOf("tier" to "premium")
        )
    )
    
    when (variant ?: assignment?.variant) {
        "control" -> showStandardCheckout()
        "treatment_a" -> showOnePageCheckout()
        "treatment_b" -> showModalCheckout()
        else -> showStandardCheckout() // Fallback
    }
}
```

> 💡 **Tip**: User ID is required for consistent experiment assignment. The same user will always get the same variant.

---

## ⚙️ Setting Up Remote Config

### Firebase Remote Config Setup

**Step 1:** Add Firebase to your project following the [official guide](https://firebase.google.com/docs/android/setup)

**Step 2:** In Firebase Console, create these parameters:

| Parameter Name | Type | Description |
|---------------|------|-------------|
| `flagship_flags` | JSON | Your feature flags |
| `flagship_experiments` | JSON | Your A/B tests |

**Step 3:** Example Firebase Console values:

```json
// flagship_flags parameter value:
{
  "new_payment_flow": true,
  "dark_mode_enabled": false,
  "api_timeout": 5000,
  "max_upload_mb": 10
}

// flagship_experiments parameter value:
{
  "checkout_test": {
    "variants": [
      { "name": "control", "weight": 0.5 },
      { "name": "variant_a", "weight": 0.5 }
    ]
  }
}
```

**Step 4:** That's it! Flagship automatically parses and uses these values.

> 📖 **Full guide**: [Firebase Integration Guide](docs/USAGE_GUIDE.md#firebase-remote-config)

### REST API Provider Setup

If you have your own backend, return JSON in this format:

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

> 📖 **Full API docs**: [REST Provider Guide](docs/USAGE_GUIDE.md#rest-provider)

---

## 🧩 Modules

| Module | Required | Description |
|--------|----------|-------------|
| `flagship-core` | ✅ **Yes** | Core library with evaluator, models, caching. Includes platform code. |
| `flagship-provider-firebase` | ⚪ Optional | Firebase Remote Config adapter |
| `flagship-provider-rest` | ⚪ Optional | Generic REST API adapter |

> 💡 **Tip**: Start with `flagship-core` + one provider. Add more modules as needed.

---

## 🔌 Advanced: Custom Providers

For connecting to proprietary backends or local files, you can implement your own provider.

> 📖 **See [Creating Custom Providers](docs/USAGE_GUIDE.md#creating-custom-providers) for detailed guide.**

Basic example:

```kotlin
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.FlagsProvider
import kotlinx.datetime.Clock

class MyCustomProvider : FlagsProvider {
    override val name: String = "my_custom_provider"
    
    override suspend fun bootstrap(): ProviderSnapshot {
        // Fetch from your backend
        val data = fetchFromMyBackend()
        
        return ProviderSnapshot(
            flags = data.flags.mapValues { (_, v) -> FlagValue.String(v.toString()) },
            experiments = emptyMap(),
            fetchedAtMs = Clock.System.now().toEpochMilliseconds()
        )
    }
    
    override suspend fun refresh(): ProviderSnapshot = bootstrap()
    
    override fun evaluateFlag(key: FlagKey, context: EvalContext): FlagValue? = null
    override fun evaluateExperiment(key: ExperimentKey, context: EvalContext): ExperimentAssignment? = null
}
```

### REST Provider JSON Schema

The `flagship-provider-rest` module expects a JSON response with the following schema:

```json
{
  "flags": {
    "feature_enabled": { "type": "bool", "value": true },
    "max_items": { "type": "int", "value": 50 }
  },
  "experiments": {
    "new_flow_exp": {
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

## 📦 SPM Support (Swift Package Manager)

Flagship can be easily integrated into any native iOS project using **Swift Package Manager**.

### 1. Adding the Package to Xcode

1. In Xcode, go to **File > Add Packages...**
2. Enter the repository URL: `https://github.com/maxluxs/Flagship` (or your separate SPM repo URL)
3. Select the version you want to install (e.g., `0.1.1`)
4. Click **Add Package**

### 2. Usage in Swift

Once installed, import the `Flagship` module in your Swift code.

#### Initialization (Recommended: Using FlagshipSwift)

```swift
import SwiftUI
import Flagship

@main
struct iOSApp: App {
    init() {
        // Quick configuration with automatic platform context initialization
        FlagshipSwift.shared.quickConfigure(
            appKey: "my-app",
            environment: "production",
            providers: [
                // Add your providers here
            ]
        )
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

#### Checking Flags

```swift
import SwiftUI
import Flagship

struct PaymentView: View {
    // Access the shared manager
    let flags = FlagshipSwift.shared.manager
    @State private var isNewPaymentEnabled = false
    
    var body: some View {
        VStack {
            if isNewPaymentEnabled {
                NewPaymentView()
            } else {
                LegacyPaymentView()
            }
        }
        .onAppear {
            // Note: Swift suspend functions are automatically converted to async/await
            Task {
                isNewPaymentEnabled = await flags.isEnabled(key: "new_payment_flow", default: false, ctx: nil)
            }
        }
    }
}
```

#### A/B Experiments

```swift
Task {
    let assignment = await flags.assign(key: "checkout_optimization_exp", ctx: nil)
    
    switch assignment?.variant {
    case "control":
        print("Show Control")
    case "treatment_a":
        print("Show A")
    default:
        print("Fallback")
    }
}
```

### 3. Generating the SPM Package (For Maintainers)

If you are maintaining this library and want to release a new version for SPM:

1. Run the Gradle task to generate the XCFramework and Package.swift:
   ```bash
   ./gradlew createSwiftPackage
   ```
2. The output will be in `spm-build/`.
3. Zip the `Flagship.xcframework`.
4. Update the `Package.swift` with the new checksum and URL.
5. Push the changes to your distribution repository.

---

## 🛠️ Debug Dashboard

Embed the debug UI in your developer settings to test flags without recompiling:

```kotlin
import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.ui.compose.FlagsDashboard

@Composable
fun DeveloperSettingsScreen() {
    FlagsDashboard(
        manager = Flagship.manager(),
        allowOverrides = true,  // Enable local overrides for testing
        allowEnvSwitch = false   // Hide environment switching
    )
}
```

> 💡 **Tip**: Use overrides to test different flag states during development without changing remote config.

---

## 📚 Documentation

**Comprehensive Guides:**
- 📖 [Usage Guide](docs/USAGE_GUIDE.md) - Complete usage guide (including detailed Android integration)
- 📱 [Simplified API Guide](docs/SIMPLIFIED_API.md) - Quick start with simplified API
- 🔄 [Migration Guide](docs/MIGRATION_GUIDE.md) - Migrating from other solutions (LaunchDarkly, Firebase, Unleash, Split.io)
- 🎯 [Use Cases](docs/USE_CASES.md) - Common use cases (A/B testing, gradual rollouts, kill switches, etc.)
- 📚 [API Reference](docs/API_REFERENCE.md) - Full API reference

**Module Documentation:**
- [flagship-core](flagship-core/README.md)
- [flagship-provider-firebase](flagship-provider-firebase/README.md)
- [flagship-provider-rest](flagship-provider-rest/README.md)
- [flagship-ui-compose](flagship-ui-compose/README.md)

**Auto-generated API Docs:**
- [Dokka HTML Documentation](https://maxluxs.github.io/Flagship/)


---

## 📐 How It Works

### Evaluation Priority

Flagship evaluates flags in this order (highest to lowest priority):

1. **Local Overrides** (set via Debug UI) - Highest priority
2. **Provider Values** (in order: Firebase → REST → Custom) - First provider with value wins
3. **Default Values** (from your code) - Fallback if no provider has the flag

### Targeting & Bucketing

- **Targeting**: Rules are evaluated **locally** (client-side). No server round-trips needed!
- **Bucketing**: Uses **MurmurHash3** with `Experiment Key + User ID` as salt. 
  - Same user = same variant (consistent across sessions)
  - Deterministic = reproducible results

---

## 🗺️ Roadmap

Future development plans:

1. **Stable API for Android and iOS**
   - Finalize and stabilize the public API
   - Ensure backward compatibility
   - Improve documentation and usage examples

2. **Web and Desktop Platform Support**
   - Add targets for JS (Web) and Native (Desktop)
   - Create artifacts for web applications
   - Support Kotlin/JS and Kotlin/Native Desktop

3. **Custom REST Server for Flags**
   - Develop a backend server for flag management
   - REST API for CRUD operations with flags and experiments
   - Administrative dashboard for management

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
