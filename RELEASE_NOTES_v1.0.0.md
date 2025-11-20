# 🎉 Flagship v1.0.0 - First Stable Release

**Flagship** is a modern, robust Kotlin Multiplatform library for **Feature Flags**, **A/B Testing**, and **Remote Configuration**.

Perfect for mobile apps built with Kotlin Multiplatform targeting Android and iOS.

---

## ✨ Key Features

### 🚀 Kotlin Multiplatform
- Native support for **Android** and **iOS**
- Share feature flag logic between platforms
- Type-safe API with Kotlin sealed classes

### 🔌 Pluggable Providers
- **Firebase Remote Config** - Out-of-the-box integration
- **REST API** - Connect to any backend
- **Custom Providers** - Implement your own data source
- Multiple providers with fallback strategies

### 🧪 A/B Testing & Experiments
- Built-in experimentation engine
- Deterministic bucketing with **MurmurHash3**
- Variant assignment with payload data
- Exposure tracking for analytics

### 🎯 Advanced Targeting
- **Region-based** targeting
- **App Version** targeting (SemVer support)
- **OS Version** targeting
- **Custom Attributes** (subscription tier, device type, etc.)
- Composite rules (AND/OR logic)

### 🛡️ Safety & Reliability
- **Offline-first** architecture
- Persistent cache (SharedPreferences/UserDefaults)
- Automatic rollback to last good snapshot
- **Thread-safe** concurrency with Mutex
- TTL-based cache invalidation

### 📊 Analytics Integration
- Exposure tracking hooks
- Integration with Google Analytics, Amplitude, Mixpanel, Segment
- Automatic experiment assignment events

### 🕵️ Debug Dashboard
- **Compose Multiplatform** debug UI
- Inspect all flags and experiments
- Force override values locally
- View provider status and diagnostics

---

## 📦 Installation

### Gradle (Android/KMP)

```kotlin
dependencies {
    implementation("io.maxluxs.flagship:flagship-core:0.1.0")
    implementation("io.maxluxs.flagship:flagship-provider-firebase:0.1.0") // Optional
    implementation("io.maxluxs.flagship:flagship-provider-rest:0.1.0")     // Optional
    implementation("io.maxluxs.flagship:flagship-ui-compose:0.1.0")        // Optional
}
```

### Swift Package Manager (iOS)

1. In Xcode: **File > Add Packages...**
2. URL: `https://github.com/MaxLuxs/Flagship`
3. Version: `0.1.0`

---

## 🚀 Quick Start

### Android (Kotlin)

```kotlin
// Initialize in Application class
val config = FlagsConfig(
    appKey = "my-app",
    environment = "production",
    providers = listOf(
        FirebaseRemoteConfigProvider(Firebase.remoteConfig)
    ),
    cache = PersistentCache(FlagsSerializer())
)
Flags.configure(config)

// Use in your app
val flags = Flags.manager()
if (flags.isEnabled("new_feature")) {
    NewFeatureScreen()
}
```

### iOS (Swift)

```swift
// Initialize in App.swift
import Flagship

@main
struct MyApp: App {
    init() {
        FlagshipIOSInitializer.shared.initialize()
    }
}

// Use in SwiftUI
let flags = Flags.shared.manager()
if flags.isEnabled(key: "new_feature", default: false, ctx: nil) {
    NewFeatureView()
}
```

---

## 📚 Documentation

- **[README](https://github.com/MaxLuxs/Flagship#readme)** - Overview and quick start
- **[Usage Guide](https://maxluxs.github.io/Flagship/USAGE_GUIDE.html)** - Complete integration guide
- **[API Reference](https://maxluxs.github.io/Flagship/API_REFERENCE.html)** - Full API documentation
- **[Migration Guide](https://maxluxs.github.io/Flagship/MIGRATION_GUIDE.html)** - Migrate from other solutions
- **[KDoc (Dokka)](https://maxluxs.github.io/Flagship/dokka/)** - Auto-generated API docs

---

## 🔧 What's Included

### Modules
- `flagship-core` - Core library with evaluator and models
- `flagship-provider-firebase` - Firebase Remote Config adapter
- `flagship-provider-rest` - REST API adapter
- `flagship-ui-compose` - Debug dashboard UI
- `flagship-platform-android` - Android-specific implementations
- `flagship-platform-ios` - iOS-specific implementations

### Supported Platforms
- ✅ Android (minSdk 21+)
- ✅ iOS (14.0+)

---

## 🎯 Use Cases

Perfect for:
- 🔄 **Feature Toggles** - Enable/disable features remotely
- 🧪 **A/B Testing** - Run experiments and compare variants
- 📊 **Gradual Rollouts** - Deploy to percentage of users
- 🛡️ **Kill Switches** - Instantly disable problematic features
- 🎯 **Remote Config** - Change app behavior dynamically
- 🚀 **KMP Projects** - Share logic between Android and iOS

---

## 🙏 Credits

Made with ❤️ by [@maxluxs](https://github.com/MaxLuxs)

---

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details.

---

## 🔗 Links

- **GitHub**: https://github.com/MaxLuxs/Flagship
- **Documentation**: https://maxluxs.github.io/Flagship/
- **Issues**: https://github.com/MaxLuxs/Flagship/issues

---

**Happy Feature Flagging! 🚩**

