# Testing Strategy for Flagship

## Overview

Flagship использует многоуровневую стратегию тестирования для обеспечения качества на всех платформах.

## Уровни тестирования

### 1. Unit Tests (в каждом модуле)

**Расположение**: `{module}/src/commonTest/` и платформо-специфичные тесты

**Цель**: Тестирование изолированной логики модулей

**Примеры**:
- Тестирование evaluator логики
- Тестирование cache механизмов
- Тестирование provider адаптеров
- Тестирование targeting rules

**Инструменты**:
- `kotlin-test` для common тестов
- `kotlinx-coroutines-test` для асинхронных операций

### 2. Integration Tests (в flagship-sample)

**Расположение**: `flagship-sample/src/commonTest/integration/`

**Цель**: Тестирование взаимодействия между модулями

**Примеры**:
- Тестирование FlagsManager с разными провайдерами
- Тестирование bootstrap/refresh flow
- Тестирование cache TTL и rollback механизмов
- Тестирование provider precedence

**Инструменты**:
- `kotlin-test`
- `kotlinx-coroutines-test`
- Mock providers для изоляции

### 3. UI Tests (в flagship-sample)

**Расположение**: `flagship-sample/src/commonTest/ui/`

**Цель**: Тестирование Compose UI компонентов

**Примеры**:
- Тестирование FlagsDashboard рендеринга
- Тестирование override interactions
- Тестирование provider selection screen

**Инструменты**:
- Compose UI Testing (экспериментальный API)
- `kotlin-test`

### 4. E2E Tests (в flagship-sample)

**Расположение**: `flagship-sample/src/commonTest/e2e/`

**Цель**: Тестирование полного flow приложения

**Примеры**:
- Тестирование инициализации Flagship
- Тестирование работы с реальными провайдерами (mock сервер)
- Тестирование offline режима
- Тестирование rollback сценариев

**Инструменты**:
- `kotlin-test`
- `kotlinx-coroutines-test`
- Mock HTTP сервер (ktor-server-test-host)

### 5. Platform-Specific Tests

**Android**: `flagship-sample/src/androidTest/`
- Instrumented тесты для Android-специфичной функциональности
- UI тесты с Espresso (если нужно)

**iOS**: Тесты через Xcode
- Unit тесты в Swift (если нужно)
- UI тесты через XCTest (если нужно)

**Desktop**: `flagship-sample/src/jvmTest/`
- JVM-специфичные тесты

**Web**: `flagship-sample/src/jsTest/`
- JS-специфичные тесты

## Структура тестов в flagship-sample

```
flagship-sample/composeApp/src/commonTest/
├── integration/
│   ├── FlagsManagerIntegrationTest.kt
│   ├── ProviderPrecedenceTest.kt
│   ├── CacheAndRollbackTest.kt
│   ├── CacheFallbackTest.kt
│   ├── BootstrapFlowTest.kt
│   ├── ExperimentTest.kt
│   ├── TargetingTest.kt
│   ├── OverrideTest.kt
│   ├── FlagValueTest.kt
│   ├── RefreshTest.kt
│   ├── ListenerTest.kt
│   └── EdgeCasesTest.kt
├── e2e/
│   ├── InitializationTest.kt
│   ├── RealProviderTest.kt
│   ├── OfflineModeTest.kt
│   └── RollbackScenarioTest.kt
└── helpers/
    ├── TestFlagsConfig.kt
    └── ComposeAppCommonTest.kt
```

## Рекомендации

1. **Используйте flagship-sample как тестовое приложение**:
   - Все integration/E2E тесты в flagship-sample
   - Это позволяет тестировать реальное использование библиотеки

2. **Mock providers для изоляции**:
   - Используйте MockFlagsProvider для unit тестов
   - Используйте mock HTTP сервер для integration тестов

3. **Платформо-независимые тесты**:
   - Пишите тесты в commonTest когда возможно
   - Используйте expect/actual для платформо-специфичных частей

4. **CI/CD**:
   - Запускайте все тесты на всех платформах в CI
   - Используйте GitHub Actions или аналогичные инструменты

5. **Coverage**:
   - Стремитесь к высокому покрытию для core модулей
   - Минимум 80% для flagship-core
   - Минимум 60% для остальных модулей

## Примеры тестов

### Unit Test (в flagship-core)
```kotlin
// flagship-core/src/commonTest/.../EvaluatorTest.kt
class EvaluatorTest {
    @Test
    fun testFlagEvaluation() {
        // test logic
    }
}
```

### Integration Test (в flagship-sample)
```kotlin
// flagship-sample/src/commonTest/integration/FlagsManagerIntegrationTest.kt
class FlagsManagerIntegrationTest {
    @Test
    fun testMultipleProviders() {
        val config = FlagsConfig(
            providers = listOf(
                MockFlagsProvider(...),
                RestFlagsProvider(...)
            )
        )
        // test provider precedence
    }
}
```

### UI Test (в flagship-sample)
```kotlin
// flagship-sample/src/commonTest/ui/FlagsDashboardTest.kt
class FlagsDashboardTest {
    @Test
    fun testDashboardRendering() {
        // compose UI test
    }
}
```

### E2E Test (в flagship-sample)
```kotlin
// flagship-sample/src/commonTest/e2e/InitializationTest.kt
class InitializationTest {
    @Test
    fun testFullInitializationFlow() {
        // test complete app flow
    }
}
```

