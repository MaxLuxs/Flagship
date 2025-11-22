# Flagship Spring Boot Starter

Spring Boot auto-configuration for Flagship feature flags.

## Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.maxluxs.flagship:flagship-spring-boot-starter:0.1.1")
    implementation("io.maxluxs.flagship:flagship-provider-rest:0.1.1")
}
```

## Configuration

Add to `application.properties`:

```properties
flagship.app-key=my-app
flagship.environment=production
```

## Usage

### Inject FlagsManager

```kotlin
@Service
class MyService(
    private val flagsManager: FlagsManager
) {
    suspend fun doSomething() {
        // Note: isEnabled is a suspend function
        if (flagsManager.isEnabled("new_ui", default = false)) {
            // Use new UI
        }
    }
}
```

### Use Flagship directly

```kotlin
@Service
class MyService {
    suspend fun doSomething() {
        // Note: All flag access methods are suspend functions
        if (Flagship.isEnabled("new_ui")) {
            // Use new UI
        }
        
        val variant = Flagship.experiment("checkout_flow")?.variant
        when (variant) {
            "control" -> showLegacyCheckout()
            "A" -> showNewCheckout()
        }
    }
}
```

### REST Endpoints

The starter provides REST endpoints:

- `GET /api/flagship/flags/{key}` - Check if flag is enabled
- `GET /api/flagship/flags/{key}/value?default=0&type=int` - Get typed flag value
- `GET /api/flagship/experiments/{key}` - Get experiment assignment
- `GET /api/flagship/flags` - Get all flags

## Custom Providers

Register custom providers as Spring beans:

```kotlin
@Configuration
class FlagshipConfig {
    @Bean
    fun restProvider(): FlagsProvider {
        val httpClient = HttpClient(CIO)
        return RestFlagsProvider(httpClient, "https://api.example.com/flags")
    }
}
```

