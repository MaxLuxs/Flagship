# Flagship JS Sample

Standalone JavaScript web application demonstrating Flagship feature flags API usage.

## Overview

This is a pure JavaScript/Kotlin JS sample that uses Flagship library via REST API. It's a simple web page that connects to a Flagship server and displays feature flags.

## Features

- ✅ Pure JavaScript/Kotlin JS implementation
- ✅ REST API integration
- ✅ Real-time flag status display
- ✅ No build tools required (uses Kotlin/JS compiler)

## Setup

1. Make sure Flagship server is running (see `flagship-server` module)
2. Build the project:
   ```bash
   ./gradlew :flagship-sample-js:jsBrowserDevelopmentWebpack
   ```
3. Open `build/dist/js/developmentExecutable/index.html` in a browser

## Configuration

The sample connects to `/api/flags` endpoint on the same origin. To change the API URL, modify `baseUrl` in `Main.kt`.

## Usage

The sample automatically:
1. Initializes Flagship with REST provider
2. Bootstraps flags from the server
3. Displays all available flags and their values
4. Shows experiment assignments (when configured)

## API Example

```kotlin
import io.maxluxs.flagship.core.Flagship
import kotlinx.coroutines.launch

// Check if feature is enabled (suspend function - use in coroutine scope)
launch {
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
launch {
    val assignment = Flagship.assign("experiment_name")
    when (assignment?.variant) {
        "control" -> showControl()
        "variant_a" -> showVariantA()
        else -> showDefault()
    }
}
```

