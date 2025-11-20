<p align="center">
  <img src="../docs/images/flagship_icon.svg" width="80" height="80" alt="Flagship Logo">
</p>

# Flagship UI Compose

A debug dashboard for **Flagship** built with Compose Multiplatform. Embed this in your app's debug menu to inspect flags, force overrides, and switch environments.

## Features

- **Flag Inspector**: View all available flags and their current values.
- **Overrides**: Force enable/disable flags locally for testing.
- **Experiment Debugging**: See which variant you are assigned to.
- **Diagnostics**: Check provider status and cache health.

## Installation

```kotlin
implementation("io.maxluxs.flagship:flagship-ui-compose:0.1.0")
```

## Usage

```kotlin
@Composable
fun DebugScreen() {
    FlagsDashboard(
        manager = Flags.manager(),
        allowOverrides = true
    )
}
```
