# Flagship Codegen

Gradle plugin for generating typed flag classes from configuration.

## Usage

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.maxluxs.flagship.codegen") version "0.1.1"
}

flagshipCodegen {
    configFile = file("flags.json")
    outputDir = file("build/generated/flagship")
    packageName = "flags"
}
```

## Configuration File

Create a `flags.json` file in your project root:

```json
{
  "flags": [
    {
      "key": "new_ui",
      "type": "BOOL",
      "description": "Enable new UI design",
      "defaultValue": "false"
    },
    {
      "key": "api_timeout",
      "type": "INT",
      "description": "API timeout in milliseconds",
      "defaultValue": "5000"
    },
    {
      "key": "welcome_message",
      "type": "STRING",
      "description": "Welcome message for users",
      "defaultValue": "\"Welcome!\""
    }
  ],
  "experiments": [
    {
      "key": "checkout_flow",
      "description": "A/B test for checkout flow",
      "variants": ["control", "A", "B"]
    }
  ]
}
```

## Generated Code

The plugin generates a `Flags` object with typed accessors using the modern Flagship API (`Flagship.value()` and `Flagship.assign()`). **Note:** All accessors are suspend functions and must be called within a coroutine scope:

```kotlin
// Boolean flag (suspend function)
lifecycleScope.launch {
    if (Flags.NewUi.enabled()) {
        showNewUI()
    }
}

// Typed values (suspend functions)
lifecycleScope.launch {
    val timeout: Int = Flags.ApiTimeout.value()
    val message: String = Flags.WelcomeMessage.value()
}

// Experiments (suspend function)
lifecycleScope.launch {
    val variant = Flags.CheckoutFlow.variant()
    when (variant) {
        "control" -> showLegacyCheckout()
        "A" -> showNewCheckout()
        "B" -> showAlternativeCheckout()
    }
}
```

## Enum Type Flags

For string flags that represent enum values, you can specify an enum type:

### 1. Define your enum

```kotlin
// In your project
enum class PaymentMethod {
    CREDIT_CARD,
    DEBIT_CARD,
    PAYPAL,
    APPLE_PAY
}
```

### 2. Configure enum flag

```json
{
  "flags": [
    {
      "key": "default_payment_method",
      "type": "STRING",
      "enumType": "com.example.PaymentMethod",
      "enumValues": ["CREDIT_CARD", "DEBIT_CARD", "PAYPAL", "APPLE_PAY"],
      "defaultValue": "\"CREDIT_CARD\"",
      "description": "Default payment method for checkout"
    }
  ]
}
```

### 3. Use typed enum accessor

```kotlin
lifecycleScope.launch {
    val method: PaymentMethod? = Flags.DefaultPaymentMethod.value()
    method?.let {
        when (it) {
            PaymentMethod.CREDIT_CARD -> showCreditCardForm()
            PaymentMethod.PAYPAL -> showPayPalButton()
            // ...
        }
    }
    
    // Or with default
    val methodWithDefault = Flags.DefaultPaymentMethod.valueOrDefault(PaymentMethod.CREDIT_CARD)
}
```

## Task

Run the generation task:

```bash
./gradlew generateFlags
```

The task runs automatically before compilation.

## Integration Examples

### Android Project

```kotlin
// build.gradle.kts
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
    id("io.maxluxs.flagship.codegen") version "0.1.1"
}

flagshipCodegen {
    configFile = file("flags.json")
    outputDir = file("build/generated/flagship")
    packageName = "com.example.app.flags"
}

dependencies {
    implementation("io.maxluxs.flagship:flagship-core:0.1.1")
    implementation("io.maxluxs.flagship:flagship-provider-firebase:0.1.1")
}

// In your Activity/Fragment
lifecycleScope.launch {
    if (Flags.NewFeature.enabled()) {
        // Show new feature
    }
}
```

### iOS Project

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("io.maxluxs.flagship.codegen") version "0.1.1"
}

flagshipCodegen {
    configFile = file("flags.json")
    outputDir = file("build/generated/flagship")
    packageName = "com.example.app.flags"
}

// In Swift code (via Kotlin/Native)
let enabled = Flags.shared.newFeature().enabled()
```

### CI/CD Integration

Add codegen to your CI pipeline:

```yaml
# GitHub Actions example
- name: Generate Flags
  run: ./gradlew generateFlags

- name: Build
  run: ./gradlew build
```

## Troubleshooting

### Generated code not found

- Ensure `generateFlags` task runs before compilation
- Check that `outputDir` is in your source sets
- Verify `packageName` matches your import statements

### JSON type not found

- Ensure the data class is `@Serializable`
- Check that the fully qualified class name is correct
- Verify the class is in a module that's accessible

### Type mismatch errors

- All generated accessors are suspend functions
- Use them within coroutine scopes (`lifecycleScope.launch`, etc.)
- Check that default values match the flag types

