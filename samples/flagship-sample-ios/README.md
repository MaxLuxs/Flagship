# Flagship iOS Sample

Standalone iOS application demonstrating Flagship feature flags using Swift Package Manager (SPM).

## Overview

This is a pure iOS/Swift app that uses Flagship library via Swift Package Manager. It demonstrates how to integrate Flagship in a native iOS project.

## Features

- ✅ Pure Swift/iOS implementation
- ✅ Swift Package Manager integration
- ✅ REST provider support
- ✅ SwiftUI interface
- ✅ Native iOS APIs

## Setup

### 1. Build Flagship Framework for SPM

First, build the Flagship framework that will be used by SPM:

```bash
./gradlew :flagship-core:buildXCFramework
```

This creates an XCFramework that can be used in SPM.

### 2. Open Xcode Project

1. Open `FlagshipSampleiOS.xcodeproj` in Xcode
2. The project is configured to use Flagship via local SPM path
3. Build and run on simulator or device

### 3. Configure API Endpoint

Update the API endpoint in `FlagshipApp.swift`:

```swift
let baseUrl = "http://localhost:8080/api/flags" // Update for your server
```

For physical device, use your computer's IP address instead of `localhost`.

## Project Structure

```
flagship-sample-ios/
├── FlagshipSampleiOS/
│   ├── FlagshipApp.swift      # App entry point with Flagship initialization
│   ├── ContentView.swift      # Main UI
│   ├── FlagsListView.swift    # Display all flags
│   └── Info.plist
├── FlagshipSampleiOS.xcodeproj
└── README.md
```

## Usage

The app automatically:
1. Initializes Flagship in `@main` app entry
2. Bootstraps flags from the server
3. Displays all available flags
4. Shows experiment assignments

## API Example

```swift
import Flagship

// Check if feature is enabled
Task {
    let enabled = await FlagshipSwift.shared.manager.isEnabled(
        key: "new_feature",
        default: false
    )
    if enabled {
        showNewFeature()
    }
}

// Get experiment assignment
let assignment = await FlagshipSwift.shared.manager.assign(
    key: "experiment_name"
)
switch assignment?.variant {
case "control":
    showControl()
case "variant_a":
    showVariantA()
default:
    showDefault()
}
```

## SPM Integration

The project uses local SPM path pointing to the built framework. In production, you would:

1. Publish the framework to a repository
2. Add it via Xcode: File > Add Packages...
3. Enter the repository URL
4. Select version

## Notes

- Requires iOS 14.0+
- Uses async/await for flag operations
- Supports both simulator and device builds

