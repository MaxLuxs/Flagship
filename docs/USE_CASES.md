# Use Cases

Complete guide to common use cases for Flagship feature flags and experiments.

---

## 📋 Table of Contents

1. [Feature Flags for A/B Testing](#feature-flags-for-ab-testing)
2. [Gradual Rollouts](#gradual-rollouts)
3. [Kill Switches](#kill-switches)
4. [Environment-Specific Configuration](#environment-specific-configuration)
5. [Multi-Provider Setup](#multi-provider-setup)
6. [Canary Releases](#canary-releases)
7. [User Segmentation](#user-segmentation)
8. [Remote Configuration](#remote-configuration)

---

## Feature Flags for A/B Testing

### Overview

Use Flagship experiments to run A/B tests and measure the impact of different features or UI variations.

### Example: Checkout Flow A/B Test

**Goal:** Test if a new one-page checkout increases conversion rates.

```kotlin
// In your checkout screen
class CheckoutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val flags = Flagship.manager()
        
        lifecycleScope.launch {
            // Get experiment assignment
            val assignment = flags.assign(
                key = "checkout_flow_exp",
                ctx = EvalContext(
                    userId = currentUser.id,
                    attributes = mapOf(
                        "subscription_tier" to currentUser.tier,
                        "region" to currentUser.region
                    )
                )
            )
            
            // Show appropriate UI based on variant
            when (assignment?.variant) {
                "control" -> {
                    // Original multi-step checkout (50% of users)
                    setContentView(R.layout.checkout_legacy)
                }
                "variant_a" -> {
                    // New one-page checkout (25% of users)
                    setContentView(R.layout.checkout_onepage)
                }
                "variant_b" -> {
                    // Alternative checkout with progress bar (25% of users)
                    setContentView(R.layout.checkout_progress)
                }
                else -> {
                    // Fallback to control
                    setContentView(R.layout.checkout_legacy)
                }
            }
        }
    }
}
```

### Tracking Conversions

```kotlin
// When user completes checkout
fun onCheckoutComplete() {
    val flags = Flagship.manager()
    
    lifecycleScope.launch {
        val assignment = flags.assign("checkout_flow_exp")
        
        // Track conversion event
        analytics.track("checkout_completed", mapOf(
            "experiment" to "checkout_flow_exp",
            "variant" to (assignment?.variant ?: "control"),
            "amount" to orderTotal
        ))
    }
}
```

### Server-Side Configuration

```json
{
  "experiments": {
    "checkout_flow_exp": {
      "variants": [
        { "name": "control", "weight": 0.5 },
        { "name": "variant_a", "weight": 0.25 },
        { "name": "variant_b", "weight": 0.25 }
      ],
      "targeting": {
        "type": "composite",
        "operator": "AND",
        "rules": [
          {
            "type": "app_version_gte",
            "version": "2.0.0"
          },
          {
            "type": "region_in",
            "regions": ["US", "CA", "GB"]
          }
        ]
      }
    }
  }
}
```

### Best Practices

1. **Always track conversions** - Use analytics to measure experiment impact
2. **Use meaningful variant names** - "variant_a" is less clear than "one_page_checkout"
3. **Set appropriate weights** - Start with 50/50 for two variants, adjust based on risk
4. **Target specific users** - Use targeting rules to test with specific segments first
5. **Monitor metrics** - Track conversion rates, revenue, and user feedback

---

## Gradual Rollouts

### Overview

Gradually roll out new features to a percentage of users to minimize risk and catch issues early.

### Example: Percentage-Based Rollout

**Goal:** Roll out a new payment method to 10% of users initially, then increase to 50%, then 100%.

```kotlin
// In your payment screen
class PaymentScreen : ComposeScreen() {
    @Composable
    fun PaymentMethodSelector() {
        val flags = Flagship.manager()
        var showNewPaymentMethod by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            // Check if user is in rollout percentage
            val assignment = flags.assign(
                key = "new_payment_method_rollout",
                ctx = EvalContext(userId = currentUser.id)
            )
            
            showNewPaymentMethod = assignment?.variant == "enabled"
        }
        
        if (showNewPaymentMethod) {
            NewPaymentMethodSelector()
        } else {
            LegacyPaymentMethodSelector()
        }
    }
}
```

### Server-Side Configuration

```json
{
  "experiments": {
    "new_payment_method_rollout": {
      "variants": [
        { "name": "enabled", "weight": 0.1 },  // 10% of users
        { "name": "disabled", "weight": 0.9 }  // 90% of users
      ]
    }
  }
}
```

### Increasing Rollout Percentage

**Week 1:** 10% rollout
```json
{ "name": "enabled", "weight": 0.1 }
```

**Week 2:** 25% rollout
```json
{ "name": "enabled", "weight": 0.25 }
```

**Week 3:** 50% rollout
```json
{ "name": "enabled", "weight": 0.5 }
```

**Week 4:** 100% rollout
```json
{ "name": "enabled", "weight": 1.0 }
```

### Using Feature Flags for Rollouts

Alternatively, use a feature flag with percentage rollout:

```kotlin
// Check if feature is enabled for this user
val flags = Flagship.manager()

lifecycleScope.launch {
    val assignment = flags.assign("new_payment_method")
    
    if (assignment?.variant == "enabled") {
        showNewPaymentMethod()
    }
}
```

### Best Practices

1. **Start small** - Begin with 1-5% of users
2. **Monitor metrics** - Watch error rates, performance, user feedback
3. **Increase gradually** - Double the percentage each week if no issues
4. **Have a rollback plan** - Be ready to disable the feature instantly
5. **Test with internal users first** - Use targeting to enable for team members

---

## Kill Switches

### Overview

Instantly disable problematic features in production without deploying a new app version.

### Example: Disable Feature on Error Spike

**Goal:** Disable a new recommendation algorithm if error rates spike.

```kotlin
// In your recommendation service
class RecommendationService {
    private val flags = Flagship.manager()
    
    suspend fun getRecommendations(userId: String): List<Item> {
        // Check kill switch before using new algorithm
        val useNewAlgorithm = flags.isEnabled("new_recommendation_algorithm")
        
        return if (useNewAlgorithm) {
            try {
                newRecommendationAlgorithm(userId)
            } catch (e: Exception) {
                // Fallback to old algorithm on error
                logger.error("New algorithm failed, using fallback", e)
                oldRecommendationAlgorithm(userId)
            }
        } else {
            // Kill switch is off, use old algorithm
            oldRecommendationAlgorithm(userId)
        }
    }
}
```

### Example: Circuit Breaker Pattern

```kotlin
class PaymentService {
    private val flags = Flagship.manager()
    private var consecutiveFailures = 0
    
    suspend fun processPayment(amount: Double): PaymentResult {
        // Check kill switch
        if (!flags.isEnabled("payment_processing")) {
            return PaymentResult.Error("Payment processing is temporarily disabled")
        }
        
        return try {
            val result = processPaymentInternal(amount)
            consecutiveFailures = 0
            result
        } catch (e: Exception) {
            consecutiveFailures++
            
            // Auto-disable if too many failures
            if (consecutiveFailures >= 5) {
                logger.error("Too many failures, disabling payment processing")
                // Admin can manually disable via kill switch
            }
            
            throw e
        }
    }
}
```

### Server-Side Configuration

```json
{
  "flags": {
    "new_recommendation_algorithm": {
      "type": "bool",
      "value": false  // Set to false to disable
    },
    "payment_processing": {
      "type": "bool",
      "value": true   // Set to false to disable payments
    }
  }
}
```

### Best Practices

1. **Place kill switches early** - Check before expensive operations
2. **Provide fallbacks** - Always have a safe fallback path
3. **Monitor proactively** - Set up alerts for error rates
4. **Test kill switches** - Regularly test that kill switches work
5. **Document kill switches** - Keep a list of all kill switches and their purposes

---

## Environment-Specific Configuration

### Overview

Use different flag values for development, staging, and production environments.

### Example: API Endpoints by Environment

**Goal:** Use different API endpoints and feature flags per environment.

```kotlin
// In Application class
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val environment = when {
            BuildConfig.DEBUG -> "development"
            BuildConfig.FLAVOR == "staging" -> "staging"
            else -> "production"
        }
        
        val config = FlagsConfig(
            appKey = "my-app",
            environment = environment,  // Different environment
            providers = listOf(
                RestFlagsProvider(
                    client = httpClient,
                    baseUrl = when (environment) {
                        "development" -> "https://dev-api.example.com/flags"
                        "staging" -> "https://staging-api.example.com/flags"
                        "production" -> "https://api.example.com/flags"
                        else -> "https://api.example.com/flags"
                    }
                )
            ),
            cache = PersistentCache(FlagsSerializer())
        )
        
        Flagship.configure(config)
    }
}
```

### Server-Side Configuration

Your backend can return different values based on the `environment` parameter:

```json
// Development environment
{
  "flags": {
    "new_feature": { "type": "bool", "value": true },
    "api_timeout": { "type": "int", "value": 10000 }
  }
}

// Production environment
{
  "flags": {
    "new_feature": { "type": "bool", "value": false },
    "api_timeout": { "type": "int", "value": 5000 }
  }
}
```

### Example: Feature Flags by Environment

```kotlin
// In your feature code
class FeatureService {
    private val flags = Flagship.manager()
    
    suspend fun shouldShowDebugMenu(): Boolean {
        // Only show in development
        // Note: Environment is set during Flagship.configure() and can be accessed via config
        return flags.isEnabled("debug_menu")
    }
}
```

### Best Practices

1. **Use environment parameter** - Pass environment to FlagsConfig
2. **Separate configs** - Use different backend URLs per environment
3. **Test in staging** - Always test flag changes in staging first
4. **Document differences** - Keep track of environment-specific flags
5. **Use feature flags for testing** - Enable experimental features only in dev/staging

---

## Multi-Provider Setup

### Overview

Use multiple providers with fallback strategy for high availability and redundancy.

### Example: Firebase + REST Fallback

**Goal:** Use Firebase as primary source, REST API as fallback.

```kotlin
// In Application class
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val config = FlagsConfig(
            appKey = "my-app",
            environment = "production",
            providers = listOf(
                // Primary: Firebase Remote Config
                FirebaseProviderFactory.create(
                    application = this,
                    defaults = mapOf(
                        "new_feature" to false,
                        "api_timeout" to 5000
                    )
                ),
                
                // Fallback: REST API
                RestFlagsProvider(
                    client = httpClient,
                    baseUrl = "https://api.example.com/flags"
                )
            ),
            cache = PersistentCache(FlagsSerializer())
        )
        
        Flagship.configure(config)
    }
}
```

### How It Works

1. **First provider (Firebase)** is tried first
2. If Firebase fails or returns no value, **second provider (REST)** is used
3. If both fail, **cached values** are used
4. If no cache, **default values** are returned

### Example: Local File + Remote Providers

```kotlin
val config = FlagsConfig(
    appKey = "my-app",
    environment = "production",
    providers = listOf(
        // Local overrides (highest priority)
        FileFlagsProvider(
            filePath = "flags.json",
            hotReload = true  // Reload on file change
        ),
        
        // Remote providers (fallback)
        RestFlagsProvider(httpClient, "https://api.example.com/flags"),
        FirebaseProviderFactory.create(application)
    ),
    cache = PersistentCache(FlagsSerializer())
)
```

### Monitoring Provider Health

```kotlin
// Check provider health
val flags = Flagship.manager() as DefaultFlagsManager

lifecycleScope.launch {
    flags.providers.forEach { provider ->
        if (provider is BaseFlagsProvider) {
            val metrics = provider.getMetrics()
            logger.info("Provider ${provider.name}: success rate = ${metrics.successRate}")
            
            if (metrics.successRate < 0.95) {
                // Alert: Provider health degraded
                sendAlert("Provider ${provider.name} has low success rate")
            }
        }
    }
}
```

### Best Practices

1. **Order by priority** - Most reliable provider first
2. **Use different sources** - Firebase + REST provides redundancy
3. **Monitor health** - Track success rates for each provider
4. **Test fallbacks** - Verify fallback works when primary fails
5. **Cache aggressively** - Use PersistentCache for offline support

---

## Canary Releases

### Overview

Release new features to a small subset of users first, then gradually expand.

### Example: Canary Release for New UI

**Goal:** Release new UI to 5% of users, monitor metrics, then expand.

```kotlin
// In your main activity
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val flags = Flagship.manager()
        
        lifecycleScope.launch {
            val assignment = flags.assign(
                key = "new_ui_canary",
                ctx = EvalContext(
                    userId = currentUser.id,
                    attributes = mapOf(
                        "user_tier" to currentUser.tier,
                        "region" to currentUser.region
                    )
                )
            )
            
            setContentView(
                if (assignment?.variant == "canary") {
                    R.layout.activity_main_new  // New UI (5% of users)
                } else {
                    R.layout.activity_main_legacy  // Legacy UI (95% of users)
                }
            )
        }
    }
}
```

### Server-Side Configuration

```json
{
  "experiments": {
    "new_ui_canary": {
      "variants": [
        { "name": "canary", "weight": 0.05 },    // 5% canary
        { "name": "legacy", "weight": 0.95 }     // 95% legacy
      ],
      "targeting": {
        "type": "composite",
        "operator": "AND",
        "rules": [
          {
            "type": "app_version_gte",
            "version": "2.0.0"
          },
          {
            "type": "attribute_equals",
            "key": "user_tier",
            "value": "premium"  // Only premium users initially
          }
        ]
      }
    }
  }
}
```

### Expanding Canary Release

**Week 1:** 5% canary (premium users only)
```json
{ "name": "canary", "weight": 0.05 }
```

**Week 2:** 10% canary (all users)
```json
{ "name": "canary", "weight": 0.10 }
```

**Week 3:** 25% canary
```json
{ "name": "canary", "weight": 0.25 }
```

**Week 4:** 50% canary
```json
{ "name": "canary", "weight": 0.50 }
```

**Week 5:** 100% canary (full rollout)
```json
{ "name": "canary", "weight": 1.0 }
```

### Best Practices

1. **Start with internal users** - Test with team members first
2. **Monitor closely** - Watch error rates, performance, user feedback
3. **Expand gradually** - Double the percentage each week
4. **Have rollback ready** - Be prepared to disable instantly
5. **Measure impact** - Track conversion rates, engagement, revenue

---

## User Segmentation

### Overview

Target specific user segments with different features or experiments.

### Example: Premium User Features

**Goal:** Show premium features only to premium users.

```kotlin
// In your feature service
class FeatureService {
    private val flags = Flagship.manager()
    
    suspend fun shouldShowPremiumFeature(user: User): Boolean {
        val assignment = flags.assign(
            key = "premium_features",
            ctx = EvalContext(
                userId = user.id,
                attributes = mapOf(
                    "subscription_tier" to user.tier,
                    "account_age_days" to user.accountAgeDays
                )
            )
        )
        
        return assignment?.variant == "enabled"
    }
}
```

### Server-Side Configuration

```json
{
  "experiments": {
    "premium_features": {
      "variants": [
        { "name": "enabled", "weight": 1.0 },
        { "name": "disabled", "weight": 0.0 }
      ],
      "targeting": {
        "type": "composite",
        "operator": "AND",
        "rules": [
          {
            "type": "attribute_equals",
            "key": "subscription_tier",
            "value": "premium"
          },
          {
            "type": "attribute_gte",
            "key": "account_age_days",
            "value": 30
          }
        ]
      }
    }
  }
}
```

### Example: Region-Based Features

```kotlin
// Show feature only in specific regions
val assignment = flags.assign(
    key = "region_specific_feature",
    ctx = EvalContext(
        userId = user.id,
        attributes = mapOf("region" to user.region)
    )
)
```

### Server-Side Configuration

```json
{
  "experiments": {
    "region_specific_feature": {
      "variants": [
        { "name": "enabled", "weight": 1.0 },
        { "name": "disabled", "weight": 0.0 }
      ],
      "targeting": {
        "type": "region_in",
        "regions": ["US", "CA", "GB"]
      }
    }
  }
}
```

### Best Practices

1. **Use meaningful attributes** - subscription_tier, region, user_type
2. **Combine rules** - Use composite targeting for complex segments
3. **Test segments** - Verify targeting works correctly
4. **Document segments** - Keep track of all user segments
5. **Monitor per segment** - Track metrics for each segment separately

---

## Remote Configuration

### Overview

Change app behavior and settings dynamically without app updates.

### Example: API Timeout Configuration

**Goal:** Adjust API timeout based on server load.

```kotlin
// In your API client
class ApiClient {
    private val flags = Flagship.manager()
    
    suspend fun makeRequest(url: String): Response {
        // Get timeout from remote config
        val timeout = flags.value("api_timeout_ms", default = 5000)
        
        return httpClient.get(url) {
            timeout {
                requestTimeoutMillis = timeout
            }
        }
    }
}
```

### Example: Feature Toggles

```kotlin
// Enable/disable features remotely
class FeatureManager {
    private val flags = Flagship.manager()
    
    suspend fun getMaxUploadSize(): Int {
        return flags.value("max_upload_size_mb", default = 10)
    }
    
    suspend fun getWelcomeMessage(): String {
        return flags.value("welcome_message", default = "Welcome!")
    }
    
    suspend fun shouldShowAds(): Boolean {
        return flags.isEnabled("show_ads")
    }
}
```

### Server-Side Configuration

```json
{
  "flags": {
    "api_timeout_ms": { "type": "int", "value": 5000 },
    "max_upload_size_mb": { "type": "int", "value": 10 },
    "welcome_message": { "type": "string", "value": "Welcome to our app!" },
    "show_ads": { "type": "bool", "value": true }
  }
}
```

### Example: A/B Testing Configuration

```kotlin
// Test different button colors
class ButtonComponent {
    private val flags = Flagship.manager()
    
    @Composable
    fun ThemedButton(text: String) {
        val assignment = remember {
            runBlocking {
                flags.assign("button_color_test")
            }
        }
        
        val color = when (assignment?.variant) {
            "red" -> Color.Red
            "blue" -> Color.Blue
            "green" -> Color.Green
            else -> Color.Blue
        }
        
        Button(
            colors = ButtonDefaults.buttonColors(containerColor = color),
            onClick = { /* ... */ }
        ) {
            Text(text)
        }
    }
}
```

### Best Practices

1. **Always provide defaults** - Never rely on remote config being available
2. **Cache aggressively** - Use PersistentCache for offline support
3. **Validate values** - Check ranges and types before using
4. **Monitor changes** - Use FlagsListener to react to config updates
5. **Test locally** - Use overrides to test different configurations

---

<p align="center">
  <b>Need help with a specific use case? Create an Issue on <a href="https://github.com/maxluxs/Flagship/issues">GitHub</a>!</b>
</p>

