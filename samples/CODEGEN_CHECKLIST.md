# ✅ Codegen Setup Checklist

## Статус: Работает через MavenLocal

### ✅ Выполнено

1. **Плагин настроен и опубликован**
   - `flagship-codegen` компилируется
   - Плагин опубликован в mavenLocal
   - Конфигурация в `build.gradle.kts` правильная

2. **Sample проекты настроены**
   - `flagship-sample` (Compose Multiplatform) - ✅ работает
   - `flagship-sample-android` (Android) - ✅ работает

3. **Генерация кода работает**
   - Файл `Flags.kt` генерируется успешно
   - Все методы доступны (sync, async, Result-based, enum)

### 📝 Инструкция для проверки

#### Шаг 1: Опубликуйте плагин (один раз)

```bash
./gradlew :flagship-codegen:publishToMavenLocal
```

#### Шаг 2: Сгенерируйте код

```bash
# Для Compose Multiplatform
./gradlew :flagship-sample:generateFlags

# Для Android
./gradlew :flagship-sample-android:generateFlags
```

#### Шаг 3: Проверьте сгенерированный файл

```bash
# Compose Multiplatform
cat samples/flagship-sample/build/generated/flagship/Flags.kt

# Android
cat samples/flagship-sample-android/build/generated/flagship/Flags.kt
```

#### Шаг 4: Используйте в коде

```kotlin
import io.maxluxs.flagship.generated.Flags

lifecycleScope.launch {
    if (Flags.NewUi.enabled()) {
        // ...
    }
    
    val timeout = Flags.ApiTimeout.value()
    val variant = Flags.CheckoutFlow.variant()
}
```

### 🔄 После изменений в codegen

Если вы изменили код в `flagship-codegen`:

```bash
./gradlew :flagship-codegen:publishToMavenLocal
```

Затем перегенерируйте код в sample проектах.

### 📚 Документация

- `CODEGEN_QUICK_START.md` - Быстрый старт
- `CODEGEN_USAGE_EXAMPLE.md` - Примеры использования
- `flagship-codegen/INTEGRATION_GUIDE.md` - Полная документация

