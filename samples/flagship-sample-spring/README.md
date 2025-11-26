# Flagship Spring Boot Sample

Spring Boot sample application demonstrating Flagship feature flags and experiments usage.

## Overview

This sample application shows how to integrate Flagship into a Spring Boot application using:
- **Flagship Spring Boot Starter** for auto-configuration
- **Mock Flags Provider** for demo purposes (no real backend required)
- **REST Controllers** exposing Flagship API endpoints
- **Demo endpoints** showing real-world usage patterns

## Features

- ✅ Feature flags evaluation (boolean, int, double, string types)
- ✅ A/B testing with experiment assignments
- ✅ REST API endpoints for flags and experiments
- ✅ Demo endpoints showing practical usage
- ✅ Automatic bootstrap on application startup
- ✅ Mock data provider (no external dependencies)

## Project Structure

```
flagship-sample-spring/
├── build.gradle.kts
├── README.md
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── io/maxluxs/flagship/samplespring/
│       │       ├── SampleSpringApplication.kt      # Main application class
│       │       ├── config/
│       │       │   └── FlagshipConfig.kt           # Flagship provider configuration
│       │       ├── controller/
│       │       │   ├── FlagsController.kt         # Flag endpoints
│       │       │   ├── ExperimentsController.kt   # Experiment endpoints
│       │       │   └── DemoController.kt          # Demo endpoints
│       │       └── service/
│       │           └── MockFlagsProvider.kt        # Mock provider implementation
│       └── resources/
│           └── application.properties              # Spring Boot configuration
```

## Setup

### Prerequisites

- JDK 11 or higher
- Gradle 7.0 or higher
- Flagship project built locally (or published artifacts)

### Build from Source

1. **Build Flagship project** (if not already built):
   ```bash
   cd /path/to/Flagship
   ./gradlew build publishToMavenLocal
   ```

2. **Build Spring Boot sample**:
   ```bash
   cd samples/flagship-sample-spring
   ./gradlew build
   ```

## Running the Application

### Using Gradle

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### Using JAR

```bash
./gradlew bootJar
java -jar build/libs/flagship-sample-spring-1.0.0.jar
```

## API Endpoints

### Feature Flags

#### List All Flags
```http
GET /api/flags
```

Response:
```json
{
  "new_feature": {
    "type": "bool",
    "value": true
  },
  "max_retries": {
    "type": "int",
    "value": 3
  },
  "welcome_message": {
    "type": "string",
    "value": "Welcome to Flagship Demo!"
  }
}
```

#### Get Flag Value
```http
GET /api/flags/{key}
```

Example:
```http
GET /api/flags/new_feature
```

Response:
```json
{
  "key": "new_feature",
  "type": "bool",
  "value": true
}
```

#### Check if Flag is Enabled
```http
GET /api/flags/{key}/enabled?default=false
```

Example:
```http
GET /api/flags/new_feature/enabled
```

Response:
```json
{
  "key": "new_feature",
  "enabled": true
}
```

### Experiments

#### Get Experiment Assignment
```http
GET /api/experiments/{key}?userId=user123&appVersion=1.0.0
```

Query Parameters:
- `userId` (optional): User ID for consistent bucketing
- `deviceId` (optional): Device ID
- `appVersion` (default: "1.0.0"): Application version
- `osName` (default: "Spring Boot"): Operating system name
- `osVersion` (default: "11"): Operating system version
- `region` (optional): User region

Example:
```http
GET /api/experiments/checkout_flow?userId=user123
```

Response:
```json
{
  "key": "checkout_flow",
  "variant": "variant_a",
  "payload": {}
}
```

### Demo Endpoints

#### Checkout Flow Demo
```http
GET /api/demo/checkout?userId=user123
```

Shows how to use experiment assignment to control checkout flow.

Response:
```json
{
  "experiment": "checkout_flow",
  "variant": "variant_a",
  "checkoutFlow": {
    "flow": "streamlined",
    "steps": ["cart", "shipping_and_payment", "confirm"],
    "features": ["auto_fill", "quick_checkout"]
  }
}
```

#### Feature Demo
```http
GET /api/demo/feature
```

Shows how to use feature flags to enable/disable features.

Response:
```json
{
  "newFeatureEnabled": true,
  "paymentEnabled": true,
  "maxRetries": 3,
  "apiTimeout": 30.0,
  "welcomeMessage": "Welcome to Flagship Demo!",
  "features": {
    "newFeature": "enabled",
    "payment": "enabled"
  }
}
```

## Code Examples

### Using FlagsManager in Service

```kotlin
@Service
class MyService(
    private val flagsManager: FlagsManager
) {
    suspend fun processRequest() {
        // Check boolean flag
        if (flagsManager.isEnabled("new_feature", default = false)) {
            // Use new feature
        }
        
        // Get typed value
        val maxRetries: Int = flagsManager.value("max_retries", default = 3)
        val apiTimeout: Double = flagsManager.value("api_timeout", default = 30.0)
        
        // Get experiment assignment
        val assignment = flagsManager.assign("checkout_flow")
        when (assignment?.variant) {
            "control" -> showLegacyCheckout()
            "variant_a" -> showStreamlinedCheckout()
            "variant_b" -> showEnhancedCheckout()
        }
    }
}
```

### Using FlagsManager in Controller

```kotlin
@RestController
class MyController(
    private val flagsManager: FlagsManager
) {
    @GetMapping("/feature")
    fun getFeature(): ResponseEntity<Map<String, Any>> {
        return runBlocking {
            val enabled = flagsManager.isEnabled("new_feature", default = false)
            ResponseEntity.ok(mapOf("enabled" to enabled))
        }
    }
}
```

### Custom Evaluation Context

```kotlin
val context = EvalContext(
    userId = "user123",
    deviceId = "device456",
    appVersion = "1.0.0",
    osName = "Spring Boot",
    osVersion = "11",
    region = "US",
    attributes = mapOf("tier" to "gold")
)

val assignment = flagsManager.assign("checkout_flow", ctx = context)
```

## Mock Data

The sample uses `MockFlagsProvider` with predefined flags and experiments:

### Flags
- `new_feature`: Boolean (true)
- `dark_mode`: Boolean (false)
- `max_retries`: Integer (3)
- `api_timeout`: Double (30.0)
- `welcome_message`: String ("Welcome to Flagship Demo!")
- `payment_enabled`: Boolean (true)

### Experiments
- `test_experiment`: Control (50%) / Treatment (50%)
- `checkout_flow`: Control (33%) / Variant A (33%) / Variant B (34%)

## Configuration

### application.properties

```properties
# Spring Boot
spring.application.name=flagship-sample-spring
server.port=8080

# Flagship
flagship.app-key=spring-sample-app
flagship.environment=development

# Logging
logging.level.io.maxluxs.flagship=DEBUG
```

## Dependencies

- **flagship-spring-boot-starter**: Auto-configuration for Flagship
- **flagship-core**: Core Flagship library
- **spring-boot-starter-web**: Spring Boot web starter
- **kotlinx-coroutines-core**: Coroutines support
- **kotlinx-serialization-json**: JSON serialization

## Testing

Run tests:
```bash
./gradlew test
```

## Troubleshooting

### Bootstrap Timeout

If bootstrap times out, check:
1. MockFlagsProvider is properly registered as a Spring bean
2. Flagship configuration in `application.properties`
3. Logs for provider errors

### Flag Not Found

If a flag returns default value:
1. Check flag key spelling
2. Verify flag exists in MockFlagsProvider
3. Check logs for evaluation errors

## Next Steps

- Replace `MockFlagsProvider` with real provider (REST, Firebase)
- Add authentication/authorization
- Implement caching strategies
- Add metrics and monitoring
- Configure production settings

## Related Documentation

- [Flagship Spring Boot Starter README](../../flagship-spring-boot-starter/README.md)
- [Flagship Core Documentation](../../flagship-core/README.md)
- [Flagship Integration Guide](../../docs/INTEGRATION_GUIDE.md)

