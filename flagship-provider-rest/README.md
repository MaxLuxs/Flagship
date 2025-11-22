<p align="center">
  <img src="../docs/images/flagship_icon.svg" width="80" height="80" alt="Flagship Logo">
</p>

# Flagship REST Provider

A generic REST provider for **Flagship**. Connects to any HTTP endpoint that returns the standard Flagship JSON schema.

## Features

- **Universal**: Works with any backend returning the correct JSON format.
- **Standard Schema**: Supports flags, experiments, and targeting rules.
- **Customizable**: Configure base URL, timeouts, and HTTP client.

## Installation

```kotlin
implementation("io.maxluxs.flagship:flagship-provider-rest:0.1.1")
```

## Usage

```kotlin
val restProvider = RestFlagsProvider(
    client = HttpClient(),
    baseUrl = "https://api.example.com/flags"
)
```

