# Flagship Codegen

Gradle plugin for generating typed flag classes from configuration.

## Usage

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.maxluxs.flagship.codegen") version "0.1.0"
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

The plugin generates a `Flags` object with typed accessors. **Note:** All accessors are suspend functions and must be called within a coroutine scope:

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

## Task

Run the generation task:

```bash
./gradlew generateFlags
```

The task runs automatically before compilation.

