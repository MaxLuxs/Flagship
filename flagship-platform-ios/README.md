<p align="center">
  <img src="../docs/images/flagship_icon.svg" width="80" height="80" alt="Flagship Logo">
</p>

# Flagship Platform iOS

iOS-specific implementations for **Flagship**.

## Features

- **PersistentCache**: Uses `NSUserDefaults` (or Keychain) to store flag snapshots offline.
- **Context Helpers**: Utilities to extract `EvalContext` from iOS `UIDevice` and Bundle.

## Installation

```kotlin
implementation("io.maxluxs.flagship:flagship-platform-ios:1.0.0")
```

