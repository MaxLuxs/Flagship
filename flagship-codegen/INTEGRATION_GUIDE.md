# Flagship Codegen Integration Guide

## Overview

Flagship Codegen генерирует типобезопасные классы для флагов и экспериментов из JSON конфигурации. Генерированный код использует `Flagship` API, который уже интегрирован в `flagship-core`.

## Текущий статус интеграции

✅ **Полностью интегрировано:**
- Кодогенерация генерирует код, использующий современный API: `Flagship.value()`, `Flagship.isEnabled()`, `Flagship.assign()`
- Эти методы реализованы в `flagship-core/src/commonMain/kotlin/io/maxluxs/flagship/core/Flagship.kt` и `FlagsExtensions.kt`
- API полностью совместим и работает
- Используется рекомендуемый API из документации

## Как использовать

### 1. Подключение плагина

В `build.gradle.kts` вашего проекта:

```kotlin
plugins {
    id("io.maxluxs.flagship.codegen") version "0.1.1"
}

flagshipCodegen {
    configFile = file("flags.json")
    outputDir = file("build/generated/flagship")
    packageName = "com.example.app.flags"
}
```

### 2. Создание конфигурации

Создайте `flags.json` в корне проекта:

```json
{
  "flags": [
    {
      "key": "new_ui",
      "type": "BOOL",
      "description": "Enable new UI design",
      "defaultValue": "false"
    },
    {
      "key": "api_timeout",
      "type": "INT",
      "defaultValue": "5000"
    }
  ],
  "experiments": [
    {
      "key": "checkout_flow",
      "variants": ["control", "A", "B"]
    }
  ]
}
```

### 3. Генерация кода

Запустите задачу генерации:

```bash
./gradlew generateFlags
```

Или код будет сгенерирован автоматически перед компиляцией.

### 4. Использование сгенерированных флагов

```kotlin
import com.example.app.flags.Flags
import kotlinx.coroutines.launch

// Boolean flag
lifecycleScope.launch {
    if (Flags.NewUi.enabled()) {
        showNewUI()
    }
}

// Typed values
lifecycleScope.launch {
    val timeout: Int = Flags.ApiTimeout.value()
    val message: String = Flags.WelcomeMessage.value()
}

// Experiments
lifecycleScope.launch {
    val variant = Flags.CheckoutFlow.variant()
    when (variant) {
        "control" -> showLegacyCheckout()
        "A" -> showNewCheckout()
        "B" -> showAlternativeCheckout()
    }
}
```

## Интеграция с Flagship API

Генерированный код использует `Flagship` object из `flagship-core`:

```kotlin
// Генерируется код вида:
suspend fun enabled(): Boolean {
    return Flagship.isEnabled("new_ui", default = false)
}

suspend fun value(): Int {
    return Flagship.value("api_timeout", default = 5000) // Использует extension функцию
}

suspend fun assignment(): ExperimentAssignment? {
    return Flagship.assign("checkout_flow") // Современный API вместо experiment()
}
```

`Flagship` API уже реализован и работает с:
- ✅ FlagsManager
- ✅ Все провайдеры (REST, Firebase, LaunchDarkly, File, Env)
- ✅ Кэширование
- ✅ Overrides
- ✅ Listeners

## Что уже работает

1. ✅ Генерация типобезопасного кода
2. ✅ Интеграция с Flagship API
3. ✅ Поддержка всех типов флагов (Bool, Int, Double, String, JSON)
4. ✅ Поддержка enum флагов
5. ✅ Поддержка экспериментов
6. ✅ Автоматическая генерация перед компиляцией

## Что можно улучшить

1. ⚠️ Подключение плагина к sample проекту (нужно настроить buildSrc или опубликовать плагин)
2. ⚠️ Примеры использования в документации
3. ⚠️ Поддержка новых типов флагов (Date, Enum, List, Map) в кодогенерации

## Заключение

Интеграция **уже работает**. Кодогенерация генерирует код, который использует существующий `Flagship` API. Нужно только:
1. Подключить плагин к проекту
2. Создать `flags.json`
3. Запустить генерацию

Все остальное уже интегрировано и работает!

