# Статус тестов Flagship

## Текущее состояние

### ✅ Исправлено
- `TestFlagsConfig.kt` - исправлен импорт `DefaultLogger`
- `TestProvider.kt` - создан тестовый провайдер с поддержкой кастомных flags
- `ExperimentTest.kt` - исправлены `EvalContext` (добавлены `osName`, `osVersion`) и `assign()` (используется `ctx =`)
- `FlagsManagerIntegrationTest.kt` - заменен `MockFlagsProvider` на `TestProvider`, исправлен `FlagsConfig`
- `InitializationTest.kt` - заменен `MockFlagsProvider` на `TestProvider`, исправлены вызовы `isEnabled()`
- `OfflineModeTest.kt` - исправлен импорт `ProviderSnapshot`

### ⚠️ Осталось исправить (~130 ошибок)

#### Основные проблемы:

1. **EvalContext** - нужно добавить `osName` и `osVersion` в:
   - `TargetingTest.kt` (множество мест)
   - `BootstrapFlowTest.kt`
   - `CacheAndRollbackTest.kt`
   - `CacheFallbackTest.kt`
   - `EdgeCasesTest.kt`
   - `FlagValueTest.kt`
   - `ListenerTest.kt`
   - `OverrideTest.kt`
   - `ProviderPrecedenceTest.kt`
   - `RefreshTest.kt`
   - `RollbackScenarioTest.kt`

2. **assign()** - заменить `context =` на `ctx =` в:
   - `TargetingTest.kt`
   - Других файлах с экспериментами

3. **ProviderSnapshot** - исправить импорты в:
   - `RefreshTest.kt`
   - `RealProviderTest.kt`
   - `RollbackScenarioTest.kt`
   - `CacheAndRollbackTest.kt`
   - `CacheFallbackTest.kt`
   - `BootstrapFlowTest.kt`

4. **FlagsConfig** - убрать параметр `flags`, добавить `cache` и `logger` в:
   - `ProviderPrecedenceTest.kt`
   - `RefreshTest.kt`
   - Других файлах

5. **MockEngine** - исправить импорты Ktor в:
   - `RealProviderTest.kt`

6. **isEnabled()** - добавить параметр `default` во всех вызовах

7. **TargetingRule** - исправить типы (List -> Set) в:
   - `TargetingTest.kt`

## Следующие шаги

1. Массово исправить все `EvalContext` (добавить `osName` и `osVersion`)
2. Массово исправить все `assign()` вызовы (`context =` -> `ctx =`)
3. Исправить импорты `ProviderSnapshot`
4. Исправить `FlagsConfig` конструкторы
5. Исправить `MockEngine` в `RealProviderTest`
6. Запустить тесты и проверить покрытие

## Команды для запуска

```bash
# Компиляция тестов
./gradlew :flagship-sample:compileTestKotlinJvm

# Запуск тестов
./gradlew :flagship-sample:jvmTest

# Проверка покрытия (если настроен JaCoCo)
./gradlew :flagship-sample:test jacocoTestReport
```

