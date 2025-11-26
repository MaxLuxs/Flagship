# flagship-provider-rest

REST API provider implementation for Flagship.

## Features

- Universal REST provider - works with any HTTP endpoint
- Standard JSON schema support
- Customizable HTTP client configuration
- Automatic snapshot parsing
- Support for flags, experiments, and targeting rules

## Quick Start

```kotlin
val provider = RestFlagsProvider(
    client = httpClient,
    baseUrl = "https://api.example.com/flags"
)

val config = FlagsConfig(
    appKey = "my-app",
    environment = "production",
    providers = listOf(provider)
)
```

