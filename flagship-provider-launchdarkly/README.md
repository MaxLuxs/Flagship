<p align="center">
  <img src="../docs/images/flagship_icon.svg" width="80" height="80" alt="Flagship Logo">
</p>

# Flagship LaunchDarkly Provider

Enterprise-grade feature flag provider powered by **LaunchDarkly**. Supports real-time updates, advanced targeting, and all LaunchDarkly features.

## Features

- **Real-time Updates**: Live flag changes via streaming
- **Advanced Targeting**: Complex user segmentation
- **Metrics & Analytics**: Built-in analytics integration
- **Enterprise Ready**: Production-grade reliability

## Installation

```kotlin
implementation("io.maxluxs.flagship:flagship-provider-launchdarkly:0.1.0")
```

## Usage

### Android (Recommended - using Factory)

The easiest way to create a LaunchDarkly provider on Android:

```kotlin
import io.maxluxs.flagship.provider.launchdarkly.LaunchDarklyProviderFactory

val provider = LaunchDarklyProviderFactory.create(
    application = application,
    mobileKey = "mob-YOUR-MOBILE-KEY",
    userId = "user-123",
    userName = "User Name",
    name = "launchdarkly"
)

val config = FlagsConfig(
    appKey = "your-app",
    environment = "production",
    providers = listOf(provider),
    cache = AndroidFlagsInitializer.createPersistentCache(context)
)

Flags.configure(config)
```

The factory handles:
- LaunchDarkly SDK initialization
- LDConfig and LDContext setup
- Client initialization (blocks until ready)

### Manual Setup (Advanced)

If you need more control, you can create the provider manually:

```kotlin
val ldClient = LDClient.init(
    application,
    LDConfig.Builder()
        .mobileKey("mob-YOUR-MOBILE-KEY")
        .build(),
    LDContext.builder("user-id")
        .kind("user")
        .name("User Name")
        .build()
).get() // Block until initialized

val provider = LaunchDarklyProvider(
    AndroidLaunchDarklyAdapter(ldClient)
)
```

### iOS

```kotlin
// Coming soon - iOS native SDK integration
```

## LaunchDarkly Setup

1. Create account at https://launchdarkly.com
2. Get your Mobile SDK key from Settings
3. Create flags with naming convention:
   - Feature flags: `new_feature`, `dark_mode`
   - Experiments: `exp_checkout_flow`
4. Configure targeting rules in LaunchDarkly dashboard

## Flag Types

LaunchDarkly supports:
- **Boolean**: true/false flags
- **String**: text values
- **Number**: integers and doubles
- **JSON**: complex objects for experiments

## Example Flags

```json
// Boolean flag
new_feature: true

// Number flag
max_retries: 3

// JSON experiment
exp_checkout_flow: {
  "variants": [
    {"name": "control", "weight": 0.5},
    {"name": "variant_a", "weight": 0.5}
  ]
}
```

## Testing

```kotlin
// Force refresh
Flags.manager().refresh()

// Check flag value
val enabled = Flags.manager().isEnabled("new_feature")

// Get experiment assignment
val assignment = Flags.manager().assign("checkout_flow")
```

