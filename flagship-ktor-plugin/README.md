# Flagship Ktor Plugin

Ktor plugin for Flagship feature flags integration.

## Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.maxluxs.flagship:flagship-ktor-plugin:0.1.0")
    implementation("io.maxluxs.flagship:flagship-provider-rest:0.1.0")
}
```

## Usage

### Install Plugin

```kotlin
import io.maxluxs.flagship.ktor.flagship
import io.maxluxs.flagship.provider.rest.RestFlagsProvider

fun Application.module() {
    val httpClient = HttpClient(CIO)
    val restProvider = RestFlagsProvider(httpClient, "https://api.example.com/flags")
    
    flagship {
        appKey = "my-app"
        environment = "production"
        providers = listOf(restProvider)
    }
    
    routing {
        get("/") {
            // Note: isEnabled is a suspend function - Ktor routes are already suspend
            if (Flagship.isEnabled("new_ui")) {
                call.respondText("New UI enabled")
            } else {
                call.respondText("Legacy UI")
            }
        }
    }
}
```

### Use in Routes

```kotlin
routing {
    get("/checkout") {
        // Note: experiment is a suspend function - Ktor routes are already suspend
        val variant = Flagship.experiment("checkout_flow")?.variant
        when (variant) {
            "control" -> call.respondText("Legacy checkout")
            "A" -> call.respondText("New checkout A")
            "B" -> call.respondText("New checkout B")
            else -> call.respondText("Default checkout")
        }
    }
}
```

### REST Endpoints

The plugin automatically adds REST endpoints:

- `GET /api/flagship/flags/{key}` - Check if flag is enabled
- `GET /api/flagship/flags/{key}/value?default=0&type=int` - Get typed flag value
- `GET /api/flagship/experiments/{key}` - Get experiment assignment
- `GET /api/flagship/flags` - Get all flags

