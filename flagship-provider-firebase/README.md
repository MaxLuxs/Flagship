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
implementation("io.maxluxs.flagship:flagship-provider-firebase:1.0.0")
```

## Usage

```kotlin
val firebaseProvider = FirebaseRemoteConfigProvider(
    // Platform specific adapter (Android/iOS)
    adapter = FirebaseAdapter(remoteConfig) 
)
```

