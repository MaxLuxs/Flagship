<p align="center">
  <img src="../docs/images/flagship_icon.svg" width="80" height="80" alt="Flagship Logo">
</p>

# Flagship Firebase Provider

A ready-to-use adapter for **Firebase Remote Config**. Allows you to use Firebase as the backend for Flagship, leveraging its dashboard while using Flagship's type-safe API and multi-provider capabilities.

## Features

- **Seamless Integration**: Wraps the native Firebase Remote Config SDKs on Android and iOS.
- **Async Fetching**: Fully supports Kotlin Coroutines.
- **JSON Support**: Automatically parses complex JSON values from Remote Config.

## Installation

```kotlin
implementation("io.maxluxs.flagship:flagship-provider-firebase:0.1.0")
```

## Usage

### Android (Recommended - using Factory)

The easiest way to create a Firebase provider on Android:

```kotlin
import io.maxluxs.flagship.provider.firebase.FirebaseProviderFactory

val firebaseProvider = FirebaseProviderFactory.create(
    application = application,
    defaults = mapOf(
        "new_feature" to false,
        "dark_mode" to false
    ),
    name = "firebase"
)
```

The factory handles:
- Firebase initialization
- Remote Config settings configuration
- Default values setup

### Manual Setup (Advanced)

If you need more control, you can create the provider manually:

```kotlin
// Initialize Firebase
if (FirebaseApp.getApps(application).isEmpty()) {
    FirebaseApp.initializeApp(application)
}

val remoteConfig = FirebaseRemoteConfig.getInstance()
// Configure settings...

val adapter = AndroidFirebaseAdapter(remoteConfig)
val firebaseProvider = FirebaseRemoteConfigProvider(adapter, name = "firebase")
```

