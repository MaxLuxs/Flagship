<p align="center">
  <img src="images/flagship_icon.svg" width="120" height="120" alt="Flagship Logo">
</p>

<h1 align="center">📚 API Reference</h1>

Полный справочник по публичному API библиотеки Flagship.

---

## Core API

### `Flags` (Singleton)

Глобальная точка доступа к Flagship.

```kotlin
object Flags {
    fun configure(config: FlagsConfig)
    fun manager(): FlagsManager
}
```

#### `configure(config: FlagsConfig)`

Инициализирует Flagship с заданной конфигурацией. Вызовите **один раз** при старте приложения.

**Parameters:**
- `config: FlagsConfig` - конфигурация библиотеки

**Throws:**
- `IllegalStateException` - если уже был вызван `configure()`

**Example:**
```kotlin
Flags.configure(
    FlagsConfig(
        appKey = "my-app",
        environment = "production",
        providers = listOf(FirebaseRemoteConfigProvider(Firebase.remoteConfig)),
        cache = InMemoryCache(),
        logger = DefaultLogger()
    )
)
```

#### `manager(): FlagsManager`

Возвращает экземпляр `FlagsManager` для работы с флагами.

**Returns:** `FlagsManager`

**Throws:**
- `IllegalStateException` - если `configure()` не был вызван

**Example:**
```kotlin
val manager = Flags.manager()
```

---

### `FlagsManager` (Interface)

Основной интерфейс для работы с флагами и экспериментами.

```kotlin
interface FlagsManager {
    // Feature Flags
    fun isEnabled(key: FlagKey, default: Boolean = false, ctx: EvalContext? = null): Boolean
    fun <T> value(key: FlagKey, default: T, ctx: EvalContext? = null): T
    
    // Experiments
    fun assign(key: ExperimentKey, ctx: EvalContext? = null): ExperimentAssignment?
    
    // Lifecycle
    suspend fun bootstrap(): Boolean
    suspend fun refresh(): Boolean
    suspend fun ensureBootstrap(timeoutMs: Long): Boolean
    
    // Overrides (Debug only)
    fun setOverride(key: FlagKey, value: FlagValue)
    fun clearOverride(key: FlagKey)
    fun listOverrides(): List<FlagKey>
    
    // Introspection
    fun listAllFlags(): Map<FlagKey, FlagValue>
    
    // Listeners
    fun addListener(listener: FlagsListener)
    fun removeListener(listener: FlagsListener)
}
```

#### `isEnabled(key, default, ctx): Boolean`

Проверяет, включён ли feature flag.

**Parameters:**
- `key: String` - ключ флага
- `default: Boolean = false` - значение по умолчанию (если флаг не найден)
- `ctx: EvalContext? = null` - контекст для таргетинга (опционально)

**Returns:** `Boolean` - `true` если флаг включён, иначе `false`

**Example:**
```kotlin
if (manager.isEnabled("dark_mode")) {
    enableDarkTheme()
}
```

#### `value<T>(key, default, ctx): T`

Получает типизированное значение флага.

**Type Parameters:**
- `T` - тип значения (String, Int, Double, Boolean, JsonElement)

**Parameters:**
- `key: String` - ключ флага
- `default: T` - значение по умолчанию
- `ctx: EvalContext? = null` - контекст для таргетинга

**Returns:** `T` - значение флага или `default` если не найден/неверный тип

**Example:**
```kotlin
val timeout: Int = manager.value("request_timeout", default = 5000)
val apiUrl: String = manager.value("api_base_url", default = "https://api.example.com")
```

#### `assign(key, ctx): ExperimentAssignment?`

Назначает пользователя в вариант эксперимента.

**Parameters:**
- `key: String` - ключ эксперимента
- `ctx: EvalContext? = null` - контекст пользователя (userId обязателен!)

**Returns:** `ExperimentAssignment?` - назначение или `null` если эксперимент не найден

**Example:**
```kotlin
val assignment = manager.assign(
    "checkout_test",
    EvalContext(userId = "user_123")
)

when (assignment?.variant) {
    "control" -> OldCheckout()
    "variant_a" -> NewCheckout()
}
```

#### `bootstrap(): Boolean`

Асинхронно загружает данные со всех провайдеров.

**Returns:** `Boolean` - `true` если загрузка успешна

**Example:**
```kotlin
lifecycleScope.launch {
    val success = manager.bootstrap()
    if (success) {
        // Данные загружены
    }
}
```

#### `refresh(): Boolean`

Принудительно обновляет данные со всех провайдеров (игнорируя TTL).

**Returns:** `Boolean` - `true` если обновление успешно

**Example:**
```kotlin
lifecycleScope.launch {
    manager.refresh()
}
```

#### `ensureBootstrap(timeoutMs): Boolean`

Гарантирует загрузку данных с таймаутом.

**Parameters:**
- `timeoutMs: Long` - таймаут в миллисекундах

**Returns:** `Boolean` - `true` если данные загружены в пределах таймаута

**Example:**
```kotlin
val success = manager.ensureBootstrap(5000) // 5 секунд
```

#### `setOverride(key, value)`

Устанавливает локальное переопределение флага (для отладки).

**Parameters:**
- `key: String` - ключ флага
- `value: FlagValue` - новое значение

**Example:**
```kotlin
manager.setOverride("dark_mode", FlagValue.Bool(true))
```

#### `clearOverride(key)`

Удаляет локальное переопределение.

**Parameters:**
- `key: String` - ключ флага

**Example:**
```kotlin
manager.clearOverride("dark_mode")
```

#### `listOverrides(): List<String>`

Возвращает список всех активных переопределений.

**Returns:** `List<String>` - ключи флагов с переопределениями

#### `listAllFlags(): Map<String, FlagValue>`

Возвращает все доступные флаги и их значения.

**Returns:** `Map<String, FlagValue>`

#### `addListener(listener)`

Добавляет слушателя изменений.

**Parameters:**
- `listener: FlagsListener`

**Example:**
```kotlin
manager.addListener(object : FlagsListener {
    override fun onSnapshotUpdated(providersCount: Int) {
        // Обновить UI
    }
    
    override fun onOverrideChanged(key: String) {
        // Override изменён
    }
})
```

#### `removeListener(listener)`

Удаляет слушателя.

**Parameters:**
- `listener: FlagsListener`

---

### `FlagsConfig` (Data Class)

Конфигурация Flagship.

```kotlin
data class FlagsConfig(
    val appKey: String,
    val environment: String,
    val providers: List<FlagsProvider>,
    val cache: FlagsCache = InMemoryCache(),
    val logger: FlagsLogger = DefaultLogger(),
    val analytics: AnalyticsAdapter? = null
)
```

**Fields:**
- `appKey: String` - уникальный идентификатор приложения
- `environment: String` - окружение ("production", "staging", "dev")
- `providers: List<FlagsProvider>` - список провайдеров (в порядке приоритета)
- `cache: FlagsCache` - кэш для offline поддержки
- `logger: FlagsLogger` - логгер для отладки
- `analytics: AnalyticsAdapter?` - адаптер для аналитики (опционально)

---

### `EvalContext` (Data Class)

Контекст пользователя для таргетинга.

```kotlin
data class EvalContext(
    val userId: String,
    val attributes: Map<String, Any> = emptyMap()
)
```

**Fields:**
- `userId: String` - уникальный ID пользователя (обязательно для экспериментов)
- `attributes: Map<String, Any>` - дополнительные атрибуты для таргетинга

**Common attributes:**
- `region: String` - код страны ("US", "GB", "RU")
- `app_version: String` - версия приложения ("2.5.0")
- `os_version: String` - версия ОС ("14.5")
- `device_type: String` - тип устройства ("phone", "tablet")
- `subscription_tier: String` - уровень подписки ("free", "premium")

**Example:**
```kotlin
val ctx = EvalContext(
    userId = "user_12345",
    attributes = mapOf(
        "region" to "US",
        "app_version" to "2.6.0",
        "subscription_tier" to "premium"
    )
)
```

---

### `FlagValue` (Sealed Class)

Типизированное значение флага.

```kotlin
sealed class FlagValue {
    data class Bool(val value: Boolean) : FlagValue()
    data class String(val value: kotlin.String) : FlagValue()
    data class Int(val value: kotlin.Int) : FlagValue()
    data class Double(val value: kotlin.Double) : FlagValue()
    data class Json(val value: JsonElement) : FlagValue()
}
```

**Example:**
```kotlin
val flag1 = FlagValue.Bool(true)
val flag2 = FlagValue.String("hello")
val flag3 = FlagValue.Int(42)
val flag4 = FlagValue.Double(3.14)
val flag5 = FlagValue.Json(buildJsonObject { put("key", "value") })
```

---

### `ExperimentAssignment` (Data Class)

Результат назначения в эксперимент.

```kotlin
data class ExperimentAssignment(
    val experimentKey: String,
    val variant: String,
    val payload: JsonObject = JsonObject(emptyMap()),
    val assignmentHash: String
)
```

**Fields:**
- `experimentKey: String` - ключ эксперимента
- `variant: String` - название варианта ("control", "variant_a", etc.)
- `payload: JsonObject` - дополнительные данные варианта
- `assignmentHash: String` - хэш назначения (для отладки)

**Example:**
```kotlin
val assignment = manager.assign("button_color_test")
println("Variant: ${assignment?.variant}") // "variant_a"
val color = assignment?.payload["color"]?.jsonPrimitive?.content // "#FF5733"
```

---

### `FlagsListener` (Interface)

Слушатель изменений флагов.

```kotlin
interface FlagsListener {
    fun onSnapshotUpdated(providersCount: Int)
    fun onOverrideChanged(key: FlagKey)
}
```

#### `onSnapshotUpdated(providersCount)`

Вызывается при обновлении snapshot'а с провайдеров.

**Parameters:**
- `providersCount: Int` - количество успешно обновлённых провайдеров

#### `onOverrideChanged(key)`

Вызывается при изменении локального override.

**Parameters:**
- `key: String` - ключ изменённого флага

---

## Providers

### `FlagsProvider` (Interface)

Интерфейс для кастомных провайдеров.

```kotlin
interface FlagsProvider {
    val name: String
    suspend fun fetch(context: EvalContext): ProviderSnapshot
}
```

### `FirebaseRemoteConfigProvider`

```kotlin
class FirebaseRemoteConfigProvider(
    private val remoteConfig: FirebaseRemoteConfig,
    private val fetchIntervalSeconds: Long = 3600
) : FlagsProvider
```

### `RestFlagsProvider`

```kotlin
class RestFlagsProvider(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : FlagsProvider
```

---

## Cache

### `FlagsCache` (Interface)

```kotlin
interface FlagsCache {
    suspend fun save(providerName: String, snapshot: ProviderSnapshot)
    suspend fun load(providerName: String): ProviderSnapshot?
    suspend fun clear(providerName: String)
    suspend fun clearAll()
}
```

### `InMemoryCache`

Хранит данные в памяти (не персистентен).

```kotlin
class InMemoryCache : FlagsCache
```

### `PersistentCache`

Хранит данные на диске (Android: SharedPreferences, iOS: UserDefaults).

```kotlin
class PersistentCache(
    private val serializer: FlagsSerializer
) : FlagsCache
```

---

## Analytics

### `AnalyticsAdapter` (Interface)

```kotlin
interface AnalyticsAdapter {
    fun trackEvent(event: AnalyticsEvent)
}
```

### `AnalyticsEvent` (Sealed Class)

```kotlin
sealed class AnalyticsEvent {
    data class ExperimentExposure(
        val experimentKey: String,
        val variant: String,
        val timestamp: Long
    ) : AnalyticsEvent()
}
```

---

## Compose UI

### `FlagsDashboard`

```kotlin
@Composable
fun FlagsDashboard(
    manager: FlagsManager,
    allowOverrides: Boolean = true,
    allowEnvSwitch: Boolean = false,
    useDarkTheme: Boolean = false
)
```

**Parameters:**
- `manager: FlagsManager` - экземпляр FlagsManager
- `allowOverrides: Boolean` - разрешить ли локальные overrides
- `allowEnvSwitch: Boolean` - показывать ли переключатель окружения
- `useDarkTheme: Boolean` - использовать тёмную тему

**Example:**
```kotlin
@Composable
fun DebugScreen() {
    FlagsDashboard(
        manager = Flags.manager(),
        allowOverrides = true,
        allowEnvSwitch = false
    )
}
```

---

<p align="center">
  <b>Полная документация API: <a href="https://maxluxs.github.io/Flagship/">Dokka HTML</a></b>
</p>

