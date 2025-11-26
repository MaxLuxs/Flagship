# Flagship Ktor Sample Application

This is a sample Ktor application demonstrating how to integrate and use Flagship feature flags and experiments.

## Overview

This sample application shows:
- How to set up Flagship with Ktor plugin
- How to use mock data provider (no real backend required)
- How to create REST endpoints for flags and experiments
- How to use flags and experiments in real-world scenarios

## Features

- **Flag Management**: List, get, and check flags via REST API
- **Experiment Assignment**: Get experiment variants via REST API
- **Demo Endpoints**: Real-world examples using flags and experiments
- **Mock Provider**: Uses in-memory mock data (no external dependencies)

## Setup

### Prerequisites

- JDK 17 or higher
- Gradle 8.0 or higher

### Installation

1. Navigate to the sample directory:
```bash
cd samples/flagship-sample-ktor
```

2. Build the project:
```bash
../../gradlew build
```

3. Run the application:
```bash
../../gradlew run
```

The server will start on `http://localhost:8080` by default.

You can customize the port and host using environment variables:
```bash
PORT=3000 HOST=127.0.0.1 ../../gradlew run
```

## API Endpoints

### Flags

#### List All Flags
```http
GET /api/flags
```

Response:
```json
{
  "new_feature": true,
  "dark_mode": false,
  "max_retries": 3,
  "api_timeout": 30.0,
  "welcome_message": "Welcome to Flagship Demo!",
  "payment_enabled": true
}
```

#### Get Flag Value
```http
GET /api/flags/{key}?default={defaultValue}
```

Example:
```http
GET /api/flags/new_feature?default=false
```

Response:
```json
{
  "key": "new_feature",
  "value": true
}
```

#### Check If Flag Enabled
```http
GET /api/flags/{key}/enabled?default=false
```

Example:
```http
GET /api/flags/payment_enabled/enabled?default=false
```

Response:
```json
{
  "key": "payment_enabled",
  "enabled": true
}
```

### Experiments

#### Get Experiment Assignment
```http
GET /api/experiments/{key}
```

Example:
```http
GET /api/experiments/checkout_flow
```

Response:
```json
{
  "key": "checkout_flow",
  "variant": "variant_a",
  "payload": null
}
```

### Demo Endpoints

#### Checkout Flow Demo
```http
GET /api/demo/checkout
```

This endpoint demonstrates using the `checkout_flow` experiment to return different checkout flows based on the assigned variant.

Response:
```json
{
  "flow": "new-a",
  "variant": "variant_a",
  "description": "New checkout flow A with multi-step wizard"
}
```

#### Feature Demo
```http
GET /api/demo/feature
```

This endpoint demonstrates using multiple flags to configure application behavior.

Response:
```json
{
  "new_feature_enabled": true,
  "payment_enabled": true,
  "max_retries": 3,
  "api_timeout_seconds": 30.0,
  "welcome_message": "Welcome to Flagship Demo!",
  "features": ["new_feature", "payment"]
}
```

## Code Examples

### Setting Up Flagship

```kotlin
import io.maxluxs.flagship.ktor.flagship
import io.maxluxs.flagship.samplektor.MockFlagsProvider

fun Application.module() {
    // Install Flagship plugin with mock provider
    flagship {
        appKey = "sample-ktor-app"
        environment = "development"
        providers = listOf(MockFlagsProvider())
    }
}
```

### Using Flags in Routes

```kotlin
import io.maxluxs.flagship.core.Flagship

routing {
    get("/checkout") {
        val enabled = Flagship.isEnabled("payment_enabled", default = false)
        if (enabled) {
            call.respond("Payment enabled")
        } else {
            call.respond("Payment disabled")
        }
    }
}
```

### Using Experiments in Routes

```kotlin
import io.maxluxs.flagship.core.Flagship

routing {
    get("/checkout") {
        val assignment = Flagship.experiment("checkout_flow")
        when (assignment?.variant) {
            "control" -> call.respond("Legacy checkout")
            "variant_a" -> call.respond("New checkout A")
            "variant_b" -> call.respond("New checkout B")
            else -> call.respond("Default checkout")
        }
    }
}
```

### Getting Typed Flag Values

```kotlin
import io.maxluxs.flagship.core.Flagship

routing {
    get("/config") {
        val maxRetries: Int = Flagship.get("max_retries", default = 3)
        val timeout: Double = Flagship.get("api_timeout", default = 30.0)
        val message: String = Flagship.get("welcome_message", default = "Hello")
        
        call.respond(mapOf(
            "maxRetries" to maxRetries,
            "timeout" to timeout,
            "message" to message
        ))
    }
}
```

## Mock Data

The sample uses `MockFlagsProvider` which provides the following mock data:

### Flags
- `new_feature`: `true` (boolean)
- `dark_mode`: `false` (boolean)
- `max_retries`: `3` (int)
- `api_timeout`: `30.0` (double)
- `welcome_message`: `"Welcome to Flagship Demo!"` (string)
- `payment_enabled`: `true` (boolean)

### Experiments
- `test_experiment`: Two variants (`control`, `treatment`) with 50/50 split
- `checkout_flow`: Three variants (`control`, `variant_a`, `variant_b`) with 33/33/34 split

## Testing

You can test the endpoints using `curl`:

```bash
# List all flags
curl http://localhost:8080/api/flags

# Get a flag value
curl http://localhost:8080/api/flags/new_feature?default=false

# Check if flag is enabled
curl http://localhost:8080/api/flags/payment_enabled/enabled

# Get experiment assignment
curl http://localhost:8080/api/experiments/checkout_flow

# Demo endpoints
curl http://localhost:8080/api/demo/checkout
curl http://localhost:8080/api/demo/feature
```

## Integration with Real Providers

To use real providers instead of mock data, replace `MockFlagsProvider` with actual providers:

```kotlin
import io.maxluxs.flagship.provider.rest.RestFlagsProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

fun Application.module() {
    val httpClient = HttpClient(CIO)
    val restProvider = RestFlagsProvider(
        httpClient = httpClient,
        baseUrl = "https://api.example.com/flags"
    )
    
    flagship {
        appKey = "my-app"
        environment = "production"
        providers = listOf(restProvider)
    }
}
```

## Project Structure

```
flagship-sample-ktor/
├── build.gradle.kts
├── README.md
└── src/
    └── main/
        └── kotlin/
            └── io/
                └── maxluxs/
                    └── flagship/
                        └── samplektor/
                            ├── Application.kt          # Ktor app entry point
                            ├── MockFlagsProvider.kt    # Mock provider implementation
                            └── routes/
                                ├── FlagsRoutes.kt      # Flag endpoints
                                ├── ExperimentsRoutes.kt # Experiment endpoints
                                └── DemoRoutes.kt       # Demo endpoints
```

## Dependencies

- **Ktor Server**: Web framework for Kotlin
- **Flagship Core**: Core Flagship library
- **Flagship Ktor Plugin**: Ktor integration for Flagship
- **Kotlinx Serialization**: JSON serialization

## Learn More

- [Flagship Documentation](../../docs/README_DOCS.md)
- [Flagship Ktor Plugin README](../../flagship-ktor-plugin/README.md)
- [Usage Guide](../../docs/USAGE_GUIDE.md)

