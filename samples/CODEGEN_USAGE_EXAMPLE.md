# Пример использования Codegen в Sample приложениях

## ✅ Настройка завершена!

Плагин успешно настроен и работает через MavenLocal.

## Быстрая проверка

### 1. Опубликуйте плагин (если еще не сделали)

```bash
./gradlew :flagship-codegen:publishToMavenLocal
```

### 2. Сгенерируйте код

```bash
./gradlew :flagship-sample:generateFlags
```

### 3. Проверьте сгенерированный файл

```bash
cat samples/flagship-sample/build/generated/flagship/Flags.kt
```

## Использование в коде

### Compose Multiplatform

```kotlin
import io.maxluxs.flagship.generated.Flags
import kotlinx.coroutines.launch

@Composable
fun MyScreen(scope: CoroutineScope) {
    var newUiEnabled by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            // Boolean flag
            newUiEnabled = Flags.NewUi.enabled()
            
            // Typed values
            val timeout = Flags.ApiTimeout.value()
            val message = Flags.WelcomeMessage.value()
            
            // Experiments
            val variant = Flags.CheckoutFlow.variant()
            when (variant) {
                Flags.CheckoutFlow.Variant.CONTROL -> // ...
                Flags.CheckoutFlow.Variant.A -> // ...
                Flags.CheckoutFlow.Variant.B -> // ...
            }
        }
    }
}
```

### Android

```kotlin
import io.maxluxs.flagship.generated.Flags

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            if (Flags.NewUi.enabled()) {
                showNewUI()
            }
            
            val timeout = Flags.ApiTimeout.value()
            val variant = Flags.CheckoutFlow.variant()
        }
    }
}
```

## Доступные методы

### Boolean Flags

```kotlin
// Async
suspend fun enabled(): Boolean

// Sync (after bootstrap)
fun enabledSync(): Boolean

// Result-based
suspend fun enabledOrError(): Result<Boolean>
```

### Typed Values (Int, String, Double)

```kotlin
// Async
suspend fun value(): Int
suspend fun intValue(default: Int = 0): Int

// Sync (after bootstrap)
fun valueSync(): Int
fun intValueSync(default: Int = 0): Int

// Result-based
suspend fun valueOrError(default: Int = 0): Result<Int>
```

### Experiments

```kotlin
// Async
suspend fun variant(): String?
suspend fun assignment(): ExperimentAssignment?

// Sync (after bootstrap)
fun variantSync(): String?
fun assignmentSync(): ExperimentAssignment?

// Enum (type-safe)
enum class Variant {
    CONTROL, A, B
}
suspend fun variantEnum(): Variant?
```

## Синхронизация с Firebase

Убедитесь, что ключи в `flags.json` соответствуют ключам в Firebase Remote Config:

- `new_ui` → `new_ui` в Firebase
- `api_timeout` → `api_timeout` в Firebase
- `checkout_flow` → `checkout_flow` в Firebase

## Перегенерация после изменений

После изменения `flags.json`:

```bash
./gradlew :flagship-sample:generateFlags
```

Или код будет перегенерирован автоматически перед компиляцией.

