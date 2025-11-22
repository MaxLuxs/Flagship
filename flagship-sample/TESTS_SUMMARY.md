# Тесты Flagship - Сводка

## Статистика

- **Всего тестовых файлов**: 18
- **Integration тестов**: 12 файлов
- **E2E тестов**: 4 файла
- **Helpers**: 2 файла
- **Всего строк кода**: ~2355

## Integration тесты

1. **FlagsManagerIntegrationTest** - базовые тесты менеджера
2. **ProviderPrecedenceTest** - приоритет провайдеров
3. **CacheAndRollbackTest** - кэш и откат
4. **CacheFallbackTest** - fallback на кэш при таймауте
5. **BootstrapFlowTest** - процесс инициализации
6. **ExperimentTest** - эксперименты и назначение вариантов
7. **TargetingTest** - правила таргетинга
8. **OverrideTest** - локальные переопределения
9. **FlagValueTest** - типизированные значения флагов
10. **RefreshTest** - обновление конфигурации
11. **ListenerTest** - уведомления об изменениях
12. **EdgeCasesTest** - граничные случаи

## E2E тесты

1. **InitializationTest** - полный flow инициализации
2. **OfflineModeTest** - офлайн режим
3. **RealProviderTest** - тесты с mock HTTP сервером
4. **RollbackScenarioTest** - сценарии отката

## Покрытие

### Основные сценарии:
- ✅ Инициализация и bootstrap
- ✅ Работа с провайдерами (multiple, precedence, fallback)
- ✅ Кэширование и откат
- ✅ Эксперименты и таргетинг
- ✅ Overrides для тестирования
- ✅ Типизированные значения флагов
- ✅ Refresh механизм
- ✅ Listeners для обновлений
- ✅ Офлайн режим
- ✅ Edge cases

### Что можно добавить:
- UI тесты для Compose компонентов
- Тесты производительности
- Тесты для realtime провайдеров
- Тесты для криптографии/подписей

## Запуск тестов

```bash
# Все тесты
./gradlew :flagship-sample:test

# Только integration тесты
./gradlew :flagship-sample:test --tests "*integration*"

# Только E2E тесты
./gradlew :flagship-sample:test --tests "*e2e*"
```

