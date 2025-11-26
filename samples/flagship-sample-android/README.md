# Flagship Android Sample

Standalone Android application demonstrating Flagship feature flags using only Android API.

## Overview

This is a pure Android app (not KMP) that uses Flagship library with Android-specific APIs. It demonstrates how to integrate Flagship in a traditional Android project.

## Features

- ✅ Pure Android implementation (no KMP)
- ✅ Android-specific Flagship APIs
- ✅ REST provider integration
- ✅ Firebase Remote Config support
- ✅ Compose UI with Flags Dashboard

## Setup

1. Make sure Flagship server is running (see `flagship-server` module)
2. For Android emulator, the app connects to `http://10.0.2.2:8080/api/flags`
3. For physical device, update the URL in `FlagshipApplication.kt` to your server IP
4. Build and run:
   ```bash
   ./gradlew :flagship-sample-android:assembleDebug
   ./gradlew :flagship-sample-android:installDebug
   ```

## Configuration

The sample uses REST provider by default. To use Firebase:
1. Add `google-services.json` to `src/main/`
2. Uncomment Firebase provider in `FlagshipApplication.kt`

## Usage

The app automatically:
1. Initializes Flagship in `Application.onCreate()`
2. Bootstraps flags from the server
3. Displays Flags Dashboard with all flags
4. Allows overrides for debugging

## API Example

```kotlin
import io.maxluxs.flagship.core.Flagship
import kotlinx.coroutines.launch

// Check if feature is enabled (suspend function - use in coroutine scope)
lifecycleScope.launch {
    if (Flagship.isEnabled("new_feature")) {
        // Show new feature
    }
    
    // Or using manager
    val manager = Flagship.manager()
    if (manager.isEnabled("new_feature")) {
        // Show new feature
    }
}

// Get experiment assignment (suspend function)
lifecycleScope.launch {
    val assignment = Flagship.assign("experiment_name")
    when (assignment?.variant) {
        "control" -> showControl()
        "variant_a" -> showVariantA()
        else -> showDefault()
    }
}
```

