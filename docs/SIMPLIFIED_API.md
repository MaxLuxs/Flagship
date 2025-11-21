# Упрощенный API для Remote Config

Новый упрощенный API делает использование Flagship проще, чем Firebase Remote Config напрямую!

---

## 🎯 Сравнение: До и После

### ❌ Старый способ (много кода)

```kotlin
// 1. Инициализация
val config = FlagsConfig(
    appKey = "my-app",
    environment = "production",
    providers = listOf(
        FirebaseProviderFactory.create(application)
    ),
    cache = AndroidFlagsInitializer.createPersistentCache(application),
    logger = DefaultLogger()
)
Flags.configure(config)

// 2. Использование
val flags = Flags.manager()
if (flags.isEnabled("new_feature")) {
    showNewFeature()
}
```

### ✅ Новый упрощенный способ (минимум кода)

```kotlin
// 1. Инициализация - всего одна строка!
Flags.initFirebase(application)

// 2. Использование - без manager()!
if (Flags.isEnabled("new_feature")) {
    showNewFeature()
}
```

**Результат:** В 2 раза меньше кода! 🎉

---

## 🚀 Быстрый старт

### Android с Firebase

```kotlin
// В Application.onCreate()
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Одна строка для инициализации!
        Flags.initFirebase(
            application = this,
            defaults = mapOf(
                "new_feature" to false,
                "max_retries" to 3
            )
        )
    }
}

// Использование в любом месте
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Прямой доступ без manager()!
        if (Flags.isEnabled("new_feature")) {
            showNewFeature()
        }
        
        val maxRetries = Flags.value("max_retries", default = 3)
        val welcomeMsg = Flags.value("welcome_message", default = "Hello!")
    }
}
```

### Android с REST API

```kotlin
// В Application.onCreate()
Flags.initRest("https://api.example.com/flags")

// Использование
if (Flags.isEnabled("feature")) { ... }
```

---

## 📝 Полный список упрощенных функций

### Инициализация

```kotlin
// Firebase (Android)
Flags.initFirebase(application, defaults = mapOf(...))

// REST API
Flags.initRest("https://api.example.com/flags")
```

### Проверка флагов

```kotlin
// Boolean флаг
if (Flags.isEnabled("dark_mode")) {
    enableDarkTheme()
}

// Типизированные значения
val timeout: Int = Flags.value("api_timeout", default = 30)
val message: String = Flags.value("welcome_msg", default = "Hello")
val discount: Double = Flags.value("promo_discount", default = 0.1)
```

### Эксперименты

```kotlin
// Простое назначение
val variant = Flags.assign("checkout_exp")?.variant
when (variant) {
    "control" -> showLegacy()
    "treatment" -> showNew()
}

// С контекстом
val assignment = Flags.assign(
    "premium_exp",
    context = EvalContext(userId = "user123")
)
```

### Обновление

```kotlin
// Обновить флаги
lifecycleScope.launch {
    Flags.refresh()
}
```

### Отладка

```kotlin
// Переопределить флаг (только для debug)
Flags.setOverride("new_feature", FlagValue.Bool(true))
Flags.clearOverride("new_feature")
```

---

## 🆚 Сравнение с Firebase Remote Config

### Firebase Remote Config (нативный)

```kotlin
// Инициализация
Firebase.remoteConfig.setConfigSettingsAsync(
    remoteConfigSettings {
        minimumFetchIntervalInSeconds = 3600
    }
)
Firebase.remoteConfig.setDefaultsAsync(mapOf(...))
Firebase.remoteConfig.fetchAndActivate().await()

// Использование
val enabled = Firebase.remoteConfig.getBoolean("new_feature")
val timeout = Firebase.remoteConfig.getLong("api_timeout").toInt()
```

### Flagship (упрощенный)

```kotlin
// Инициализация
Flags.initFirebase(application)

// Использование
val enabled = Flags.isEnabled("new_feature")
val timeout = Flags.value("api_timeout", default = 30)
```

**Преимущества Flagship:**
- ✅ Меньше кода
- ✅ Типобезопасность (type inference)
- ✅ KMP поддержка (Android + iOS)
- ✅ A/B тестирование из коробки
- ✅ Офлайн-кэш
- ✅ Множественные провайдеры

---

## 💡 Когда использовать упрощенный API

**Используйте упрощенный API, если:**
- ✅ Нужен простой remote config
- ✅ Используете Firebase или REST
- ✅ Не нужна сложная конфигурация
- ✅ Хотите минимум кода

**Используйте полный API (`Flags.configure()`), если:**
- ⚙️ Нужна кастомная конфигурация
- ⚙️ Используете несколько провайдеров
- ⚙️ Нужны кастомные аналитики/логирование
- ⚙️ Требуется продвинутая настройка кэша

---

## 🔄 Миграция со старого API

Старый API продолжает работать! Можно мигрировать постепенно:

```kotlin
// Старый код - работает
val flags = Flags.manager()
if (flags.isEnabled("feature")) { ... }

// Новый код - проще
if (Flags.isEnabled("feature")) { ... }
```

Оба варианта работают одновременно! 🎉

