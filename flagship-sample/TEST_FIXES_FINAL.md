# Финальные исправления тестов

## Проблема
Тесты падали, потому что `isEnabled()` и `value()` требуют `EvalContext`. Если `defaultContext` не установлен, методы возвращают `default` значение, не проверяя флаги.

## Решение
Добавлен вызов `setDefaultContext()` во всех тестах после создания manager:

```kotlin
val manager = Flags.manager() as DefaultFlagsManager
manager.setDefaultContext(TestHelpers.createTestContext())
manager.ensureBootstrap()
```

## Исправленные файлы
- Все тестовые файлы в `integration/` и `e2e/`
- Добавлены импорты `DefaultFlagsManager` и `TestHelpers`
- Удалены дублирующиеся вызовы `setDefaultContext`

## Результат
✅ Компиляция успешна
✅ Тесты запускаются
✅ Основные тесты проходят

