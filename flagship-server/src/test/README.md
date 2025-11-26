# Flagship Server Tests

## Unit Tests

### AuthServiceTest
Tests for authentication service:
- Password hashing and verification
- JWT token generation and verification
- Token validation

### ValidationUtilsTest
Tests for validation utilities:
- Email validation
- Password validation (complexity requirements)
- Slug validation
- Flag key validation
- UUID validation

## Running Tests

```bash
./gradlew :flagship-server:test
```

## Adding New Tests

1. Create test file in appropriate package under `src/test/kotlin/`
2. Use Kotlin Test framework
3. Follow naming convention: `*Test.kt`
4. Add descriptive test names

## Integration Tests

Integration tests with real database should use Testcontainers:
- Add `testcontainers` dependency
- Use PostgreSQL container for testing
- Test full API endpoints with HTTP client

