# Flagship Security

Security features for Flagship feature flags.

## API Keys

API keys provide access control with different permission levels:

- **READ_ONLY**: Can only read flags (for client applications)
- **ADMIN**: Can read and modify flags (for backend/admin tools)

### Usage

```kotlin
// Create API key validator
val validator = InMemoryApiKeyValidator(
    mapOf(
        "sk_live_abc123" to ApiKey(
            key = "sk_live_abc123",
            role = ApiKeyRole.READ_ONLY,
            appKey = "my-app"
        )
    )
)

// Validate API key
val apiKey = validator.validate("sk_live_abc123")
if (apiKey != null && apiKey.canRead()) {
    // Allow access
}
```

## Signature Validation

Verify that snapshots haven't been tampered with using cryptographic signatures.

### Usage

```kotlin
// Create signature validator
val crypto = MyCryptoImplementation() // Your crypto implementation
val serializer = FlagsSerializer()
val validator = SignatureValidator(crypto, serializer)

// Verify snapshot
val snapshot = provider.bootstrap()
if (snapshot.signature != null) {
    val isValid = validator.verifyHash(snapshot, snapshot.signature)
    if (!isValid) {
        throw SecurityException("Invalid signature")
    }
}
```

### Integration

Signature validation is automatically performed in `DefaultFlagsManager` when:
- Loading snapshots from providers
- Refreshing snapshots
- Loading from cache

To enable, provide a `Crypto` implementation in `FlagsConfig`:

```kotlin
val config = FlagsConfig(
    appKey = "my-app",
    environment = "production",
    providers = listOf(provider),
    cache = cache,
    crypto = MyCryptoImplementation() // Enables signature validation
)
```

## Best Practices

1. **Always use API keys in production** - Never allow unauthenticated access
2. **Use READ_ONLY keys for client apps** - Minimize attack surface
3. **Rotate API keys regularly** - Set expiration dates
4. **Enable signature validation** - Protect against tampering
5. **Use HTTPS** - Encrypt data in transit

