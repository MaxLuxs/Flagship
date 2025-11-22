# Исправления тестов - Сводка

## Статус
- **Всего ошибок**: ~148
- **Исправлено**: ~20
- **Осталось**: ~128

## Основные проблемы

1. **EvalContext** - нужно добавить `osName` и `osVersion` (обязательные параметры)
2. **assign()** - использовать `ctx =` вместо `context =`
3. **ProviderSnapshot** - правильный импорт `io.maxluxs.flagship.core.model.ProviderSnapshot`
4. **FlagsConfig** - убрать параметр `flags`, добавить `cache` и `logger` где нужно
5. **MockEngine** - исправить импорты Ktor для mock HTTP
6. **assertEquals** - добавить `import kotlin.test.assertEquals`
7. **TestProvider** - использовать вместо `MockFlagsProvider` с параметрами

## Паттерны исправлений

### EvalContext
```kotlin
// Было:
EvalContext(
    userId = "test",
    appVersion = "1.0.0"
)

// Должно быть:
EvalContext(
    userId = "test",
    appVersion = "1.0.0",
    osName = "Test",
    osVersion = "1.0"
)
```

### assign()
```kotlin
// Было:
manager.assign("exp", context = ctx)

// Должно быть:
manager.assign("exp", ctx = ctx)
```

### FlagsConfig
```kotlin
// Было:
FlagsConfig(
    appKey = "test",
    environment = "test",
    providers = listOf(...),
    flags = mapOf(...) // ❌ не существует
)

// Должно быть:
FlagsConfig(
    appKey = "test",
    environment = "test",
    providers = listOf(...),
    cache = InMemoryCache(),
    logger = DefaultLogger()
)
```

## Следующие шаги
1. Исправить все EvalContext в тестах
2. Исправить все assign() вызовы
3. Исправить ProviderSnapshot импорты
4. Исправить MockEngine в RealProviderTest
5. Добавить недостающие импорты

