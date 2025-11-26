# Пример использования Codegen с Firebase

Этот документ показывает, как использовать flagship-codegen в sample приложениях с Firebase Remote Config.

## Настройка

### 1. Добавьте плагин в build.gradle.kts

Для Compose Multiplatform приложения (`flagship-sample`):

```kotlin
buildscript {
    dependencies {
        classpath(projects.flagshipCodegen)
    }
}
apply(plugin = "io.maxluxs.flagship.codegen")

flagshipCodegen {
    configFile = file("flags.json")
    outputDir = file("build/generated/flagship")
    packageName = "io.maxluxs.flagship.generated"
}

kotlin {
    sourceSets {
        val commonMain by getting {
            kotlin.srcDirs("build/generated/flagship")
        }
    }
}
```

Для Android приложения (`flagship-sample-android`):

```kotlin
buildscript {
    dependencies {
        classpath(projects.flagshipCodegen)
    }
}
apply(plugin = "io.maxluxs.flagship.codegen")

flagshipCodegen {
    configFile = file("../flagship-sample/flags.json")
    outputDir = file("build/generated/flagship")
    packageName = "io.maxluxs.flagship.generated"
}

android {
    sourceSets {
        getByName("main") {
            java.srcDirs("build/generated/flagship")
        }
    }
}
```

### 2. Создайте flags.json

Файл `flags.json` уже существует в `samples/flagship-sample/flags.json`:

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
      "description": "API timeout in milliseconds",
      "defaultValue": "5000"
    },
    {
      "key": "welcome_message",
      "type": "STRING",
      "description": "Welcome message for users",
      "defaultValue": "\"Welcome!\""
    }
  ],
  "experiments": [
    {
      "key": "checkout_flow",
      "description": "A/B test for checkout flow",
      "variants": ["control", "A", "B"]
    },
    {
      "key": "payment_method",
      "description": "Test different payment methods",
      "variants": ["control", "new_method"]
    }
  ]
}
```

### 3. Генерация кода

Запустите задачу генерации:

```bash
./gradlew :flagship-sample:generateFlags
# или для Android
./gradlew :flagship-sample-android:generateFlags
```

Код будет автоматически сгенерирован перед компиляцией.

## Использование с Firebase

### Инициализация с Firebase

```kotlin
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.maxluxs.flagship.core.Flagship
import io.maxluxs.flagship.core.FlagsConfig
import io.maxluxs.flagship.provider.firebase.FirebaseRemoteConfigProvider
import io.maxluxs.flagship.core.platform.AndroidFlagsInitializer

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Инициализируем Firebase
        Firebase.initialize(this)
        val remoteConfig = Firebase.remoteConfig
        
        // Создаем Firebase provider
        val firebaseProvider = FirebaseRemoteConfigProvider(remoteConfig)
        
        // Настраиваем Flagship
        val config = FlagsConfig(
            appKey = "my-app",
            environment = "production",
            providers = listOf(firebaseProvider),
            cache = AndroidFlagsInitializer.createPersistentCache(this)
        )
        
        Flagship.configure(config)
        
        // Bootstrap флаги
        lifecycleScope.launch {
            Flagship.bootstrap()
        }
    }
}
```

### Использование сгенерированных классов

После генерации кода, используйте типобезопасные классы:

```kotlin
import io.maxluxs.flagship.generated.Flags
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            // Boolean flag
            if (Flags.NewUi.enabled()) {
                showNewUI()
            } else {
                showLegacyUI()
            }
            
            // Typed values
            val timeout = Flags.ApiTimeout.value()
            val message = Flags.WelcomeMessage.value()
            
            // Experiments
            val variant = Flags.CheckoutFlow.variant()
            when (variant) {
                Flags.CheckoutFlow.Variant.CONTROL -> showLegacyCheckout()
                Flags.CheckoutFlow.Variant.A -> showNewCheckout()
                Flags.CheckoutFlow.Variant.B -> showAlternativeCheckout()
            }
        }
    }
}
```

### Синхронные методы (после bootstrap)

После завершения bootstrap, можно использовать синхронные методы:

```kotlin
// После bootstrap
if (Flags.NewUi.enabledSync()) {
    showNewUI()
}

val timeout = Flags.ApiTimeout.valueSync()
val variant = Flags.CheckoutFlow.variantSync()
```

### Result-based API

Для обработки ошибок:

```kotlin
lifecycleScope.launch {
    Flags.NewUi.enabledOrError().fold(
        onSuccess = { enabled ->
            if (enabled) showNewUI()
        },
        onFailure = { error ->
            // Обработать ошибку
            Log.e("Flags", "Failed to get flag", error)
        }
    )
}
```

## Примеры в коде

### Compose Multiplatform

См. `samples/flagship-sample/composeApp/src/commonMain/kotlin/io/maxluxs/flagship/sample/CodegenExample.kt`

### Android

См. `samples/flagship-sample-android/src/main/java/io/maxluxs/flagship/sampleandroid/CodegenExampleActivity.kt`

## Преимущества codegen

1. **Типобезопасность** - компилятор проверяет правильность использования флагов
2. **Автодополнение** - IDE подсказывает доступные флаги
3. **Рефакторинг** - переименование флагов обновляет все использования
4. **Документация** - описания флагов доступны в коде
5. **Enum для экспериментов** - типобезопасные варианты экспериментов

## Синхронизация с Firebase

Убедитесь, что ключи флагов в `flags.json` соответствуют ключам в Firebase Remote Config:

- `new_ui` → `new_ui` в Firebase
- `api_timeout` → `api_timeout` в Firebase
- `checkout_flow` → `checkout_flow` в Firebase

## Дополнительная информация

См. `flagship-codegen/INTEGRATION_GUIDE.md` для подробной документации.

