<p align="center">
  <img src="images/flagship_icon.svg" width="120" height="120" alt="Flagship Logo">
</p>

<h1 align="center">🔄 Migration Guide</h1>

Guide for migrating from other solutions to Flagship.

---

## 📋 Table of Contents

1. [Migration from Firebase Remote Config](#migration-from-firebase-remote-config)
2. [Migration from LaunchDarkly](#migration-from-launchdarkly)
3. [Migration from Split.io](#migration-from-splitio)
4. [Migration from Custom Solution](#migration-from-custom-solution)

---

## Migration from Firebase Remote Config

### Before (Pure Firebase)

```kotlin
// Initialization
val remoteConfig = Firebase.remoteConfig
remoteConfig.setConfigSettingsAsync(
    remoteConfigSettings {
        minimumFetchIntervalInSeconds = 3600
    }
)

// Get values
val newFeatureEnabled = remoteConfig.getBoolean("new_feature")
val apiTimeout = remoteConfig.getLong("api_timeout").toInt()

// Fetch
remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
    if (task.isSuccessful) {
        // Updated
    }
}
```

### After (Flagship + Firebase Provider)

```kotlin
// Initialization
val config = FlagsConfig(
    appKey = "my-app",
    environment = "production",
    providers = listOf(
        FirebaseRemoteConfigProvider(
            remoteConfig = Firebase.remoteConfig,
            fetchIntervalSeconds = 3600
        )
    ),
    cache = PersistentCache(FlagsSerializer())
)
Flags.configure(config)

// Get values
val manager = Flags.manager()
val newFeatureEnabled = manager.isEnabled("new_feature")
val apiTimeout: Int = manager.value("api_timeout", default = 5000)

// Fetch (automatic with coroutines)
lifecycleScope.launch {
    manager.refresh()
}
```

### Benefits

✅ **Type-safe API** - no manual type casting  
✅ **Kotlin Coroutines** - instead of callbacks  
✅ **Offline-first** - automatic caching  
✅ **Multi-provider** - add REST fallback  
✅ **A/B Testing** - built-in experiment support

---

## Migration from LaunchDarkly

### Before (LaunchDarkly)

```kotlin
val client = LDClient.init(application, ldConfig, ldUser)

// Feature flag
val showNewUI = client.boolVariation("show-new-ui", false)

// Experiment
val buttonColor = client.stringVariation("button-color-test", "blue")
```

### After (Flagship)

```kotlin
// Initialization
val config = FlagsConfig(
    appKey = "my-app",
    environment = "production",
    providers = listOf(
        RestFlagsProvider(httpClient, "https://your-backend.com/flags")
    ),
    cache = PersistentCache(FlagsSerializer())
)
Flags.configure(config)

// Feature flag
val manager = Flags.manager()
val showNewUI = manager.isEnabled("show-new-ui")

// Experiment
val assignment = manager.assign("button-color-test")
val buttonColor = assignment?.payload["color"]?.jsonPrimitive?.content ?: "blue"
```

---

## Migration from Split.io

### Before (Split.io)

```kotlin
val factory = SplitFactoryBuilder.build("YOUR_SDK_KEY", applicationContext)
val client = factory.client()

// Feature flag
val treatment = client.getTreatment("user123", "new_checkout")
if (treatment == "on") {
    // Show new checkout
}
```

### After (Flagship)

```kotlin
val config = FlagsConfig(
    appKey = "my-app",
    environment = "production",
    providers = listOf(
        RestFlagsProvider(httpClient, "https://your-backend.com/flags")
    ),
    cache = PersistentCache(FlagsSerializer())
)
Flags.configure(config)

val manager = Flags.manager()
val ctx = EvalContext(userId = "user123")

// Feature flag
if (manager.isEnabled("new_checkout")) {
    // Show new checkout
}
```

---

## Migration from Custom Solution

### Before (Custom SharedPreferences)

```kotlin
val prefs = context.getSharedPreferences("feature_flags", Context.MODE_PRIVATE)

fun isFeatureEnabled(key: String): Boolean {
    return prefs.getBoolean(key, false)
}

fun updateFlags() {
    // Manual API call
    apiService.getFlags().enqueue(object : Callback<FlagsResponse> {
        override fun onResponse(call: Call<FlagsResponse>, response: Response<FlagsResponse>) {
            response.body()?.let { flags ->
                prefs.edit {
                    flags.features.forEach { (key, value) ->
                        putBoolean(key, value)
                    }
                }
            }
        }
        
        override fun onFailure(call: Call<FlagsResponse>, t: Throwable) {
            // Handle error
        }
    })
}
```

### After (Flagship)

```kotlin
// One-time setup
val config = FlagsConfig(
    appKey = "my-app",
    environment = "production",
    providers = listOf(
        RestFlagsProvider(httpClient, "https://your-backend.com/flags")
    ),
    cache = PersistentCache(FlagsSerializer()) // Automatic cache
)
Flags.configure(config)

// Use everywhere
val manager = Flags.manager()

fun isFeatureEnabled(key: String): Boolean {
    return manager.isEnabled(key)
}

// Update is automatic based on TTL, or manually:
suspend fun updateFlags() {
    manager.refresh()
}
```

### Benefits

✅ **Less code** - no manual SharedPreferences management  
✅ **Type-safe** - automatic type checking  
✅ **Coroutines** - modern async approach  
✅ **TTL** - automatic scheduled updates  
✅ **Multi-provider** - easy to add Firebase/other sources  
✅ **A/B Testing** - built-in experiment support

---

## Step-by-Step Migration

### Step 1: Add Flagship in Parallel

Don't remove the old solution immediately! Use both in parallel:

```kotlin
// Old
val oldFlag = remoteConfig.getBoolean("new_feature")

// New (Flagship)
val newFlag = Flags.manager().isEnabled("new_feature")

// Use new flag, fallback to old if needed
val featureEnabled = if (flagshipInitialized) newFlag else oldFlag
```

### Step 2: Switch Flags Gradually

```kotlin
// Week 1: 10% users
if (userId.hashCode() % 100 < 10) {
    useFlagship = true
}

// Week 2: 50% users
if (userId.hashCode() % 100 < 50) {
    useFlagship = true
}

// Week 3: 100% users
useFlagship = true
```

### Step 3: Monitor Errors

```kotlin
val config = FlagsConfig(
    // ...
    logger = object : FlagsLogger {
        override fun log(level: LogLevel, message: String, error: Throwable?) {
            // Send to Crashlytics/Sentry
            if (level == LogLevel.ERROR) {
                Crashlytics.getInstance().recordException(error ?: Exception(message))
            }
        }
    }
)
```

### Step 4: Remove Old Code

After successful migration (2-4 weeks):
1. Remove old SDK dependencies
2. Remove old solution code
3. Update documentation

---

<p align="center">
  <b>Need migration help? Create an Issue on <a href="https://github.com/maxluks/Flagship/issues">GitHub</a>!</b>
</p>
