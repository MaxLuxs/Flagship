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

<p align="center">
  It is designed to be <b>backend-agnostic</b>, supporting Firebase Remote Config, custom REST APIs, and local evaluations out of the box.
</p>

<p align="center">
  <i>Keywords: feature flags, feature toggles, feature switches, remote config, A/B testing, experiments, kotlin multiplatform, KMP, Android, iOS, gradual rollout, kill switch</i>
</p>

<p align="center">
  <a href="http://kotlinlang.org"><img src="https://img.shields.io/badge/kotlin-1.9.20-blue.svg?logo=kotlin" alt="Kotlin"></a>
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
- **🕵️ Debug Dashboard**: A drop-in **Compose Multiplatform** UI for inspecting flags, forcing overrides, and diagnostics.

---

## 📦 Installation

Add the dependencies to your `build.gradle.kts`:

```kotlin
// Common Main
dependencies {
    implementation("io.maxluxs.flagship:flagship-core:1.0.0")
    implementation("io.maxluxs.flagship:flagship-provider-firebase:1.0.0") // Optional
    implementation("io.maxluxs.flagship:flagship-provider-rest:1.0.0")     // Optional
    implementation("io.maxluxs.flagship:flagship-ui-compose:1.0.0")        // Optional: Debug UI
}

// Android Main
dependencies {
    implementation("io.maxluxs.flagship:flagship-platform-android:1.0.0")
}

// iOS Main
dependencies {
    implementation("io.maxluxs.flagship:flagship-platform-ios:1.0.0")
}
```

---

## 🚀 Quick Start

### 1. Initialize

Configure the library in your Application class (Android) or App Delegate (iOS).

```kotlin
        val config = FlagsConfig(
            appKey = "my-app",
    environment = "production",
            providers = listOf(
        // High priority: Firebase
        FirebaseRemoteConfigProvider(FirebaseAdapter(remoteConfig)),
        // Fallback: Custom REST API
        RestFlagsProvider(httpClient, "https://api.myserver.com/flags")
            ),
    cache = PersistentCache(platformContext),
    logger = DefaultLogger()
        )
        
        Flags.configure(config)
```

### 2. Use Feature Flags

```kotlin
    val flags = Flags.manager()
    
// Simple Boolean check
if (flags.isEnabled("new_payment_flow")) {
    ShowNewPayment()
    } else {
    ShowLegacyPayment()
}

// Typed values (Int, String, Double, JSON)
val maxUploadSize = flags.value("max_upload_mb", default = 10)
```

### 3. Run A/B Experiments

```kotlin
// Deterministic assignment based on User ID
val assignment = flags.assign("checkout_optimization_exp")

when (assignment?.variant) {
    "control" -> ShowStandardCheckout()
    "treatment_a" -> ShowOnePageCheckout()
    "treatment_b" -> ShowModalCheckout()
    else -> ShowStandardCheckout() // Fallback
}
```

---

## 🧩 Modules

| Module | Description |
|--------|-------------|
| `:flagship-core` | The brain. Evaluator, models, caching logic. |
| `:flagship-provider-firebase` | Adapter for Firebase Remote Config. |
| `:flagship-provider-rest` | Generic REST API adapter. |
| `:flagship-ui-compose` | Debug UI dashboard (Inspect & Override). |
| `:flagship-platform-android` | Android implementation (SharedPreferences). |
| `:flagship-platform-ios` | iOS implementation (UserDefaults). |

---

## 🔌 Creating Custom Providers

You can implement your own provider by implementing the `FlagsProvider` interface. This is useful for connecting to proprietary backends, local files, or other services.

```kotlin
class MyCustomProvider(
    override val name: String = "my_custom_provider"
) : FlagsProvider {
    
    // Called on app start or when refresh() is requested
    override suspend fun bootstrap(): ProviderSnapshot {
        // Fetch data from your source
        val myData = fetchFromMyBackend()
        
        // Map to Flagship models
        return ProviderSnapshot(
            flags = myData.flags.mapValues { ... },
            experiments = myData.experiments.mapValues { ... },
            fetchedAtMs = Clock.System.now().toEpochMilliseconds()
        )
    }
    
    override suspend fun refresh(): ProviderSnapshot = bootstrap()
    
    // Optional: Optimized single-flag evaluation if needed
    override fun evaluateFlag(key: FlagKey, context: EvalContext): FlagValue? {
        return null // Return null to fallback to standard evaluation using the snapshot
    }
    
    override fun evaluateExperiment(key: ExperimentKey, context: EvalContext): ExperimentAssignment? {
        return null
    }
}
```

### Using the REST Provider

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
3. Select the version you want to install (e.g., `1.0.0`)
4. Click **Add Package**

### 2. Usage in Swift

Once installed, import the `Flagship` module in your Swift code.

#### Initialization (AppDelegate or App.swift)

```swift
import SwiftUI
import Flagship

@main
struct iOSApp: App {
    init() {
        // Initialize Flagship
        FlagshipIOSInitializer.shared.initialize()
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
    let flags = Flags.shared.manager()
    
    var body: some View {
        VStack {
            if flags.isEnabled(key: "new_payment_flow", default: false, ctx: nil) {
                NewPaymentView()
            } else {
                LegacyPaymentView()
            }
        }
    }
}
```

#### A/B Experiments

```swift
let assignment = flags.assign(key: "checkout_optimization_exp", ctx: nil)

switch assignment?.variant {
case "control":
    print("Show Control")
case "treatment_a":
    print("Show A")
default:
    print("Fallback")
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

Embed the debug UI in your developer settings to easily test different flag states without recompiling.

```kotlin
@Composable
fun DeveloperSettingsScreen() {
    FlagsDashboard(
        manager = Flags.manager(),
        allowOverrides = true, // Enable local overrides
        allowEnvSwitch = false
    )
}
```

---

## 📚 Documentation

**Comprehensive Guides:**
- 📖 [Usage Guide](docs/USAGE_GUIDE.md) - Полное руководство по использованию
- 🔄 [Migration Guide](docs/MIGRATION_GUIDE.md) - Миграция с других решений
- 📚 [API Reference](docs/API_REFERENCE.md) - Справочник по API
- 🚀 [Publishing Guide](PUBLISHING.md) - Как опубликовать библиотеку
- 🔧 [Development Log](DEV_LOG.md) - История разработки

**Module Documentation:**
- [flagship-core](flagship-core/README.md)
- [flagship-provider-firebase](flagship-provider-firebase/README.md)
- [flagship-provider-rest](flagship-provider-rest/README.md)
- [flagship-ui-compose](flagship-ui-compose/README.md)
- [flagship-platform-android](flagship-platform-android/README.md)
- [flagship-platform-ios](flagship-platform-ios/README.md)

**Auto-generated API Docs:**
- [Dokka HTML Documentation](https://maxluxs.github.io/Flagship/)

---

## 📐 Architecture

Flagship uses a **Composite Provider** strategy.
1. **Overrides**: Local overrides (set via Debug UI) have the highest priority.
2. **Providers**: Iterates through the list of providers (e.g., Firebase -> REST). The first one to return a value wins.
3. **Defaults**: If no provider has the value, the code default is returned.

### Targeting & Bucketing
- **Targeting**: Rules are evaluated locally (Client-side evaluation). This supports instant updates if the rules are fetched with the config.
- **Bucketing**: Uses **MurmurHash3** with the Experiment Key + User ID as salt. This ensures that a user always sees the same variant across sessions and platforms, provided the User ID is stable.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
