# Flagship Admin UI Compose

Compose Multiplatform админ-панель для управления Flagship сервером.

## Возможности

- ✅ **Multiplatform**: Android, iOS, Desktop (JVM), Web (JS)
- ✅ **Material 3 дизайн**: Современный UI
- ✅ **Аутентификация**: Login/Register
- ✅ **Управление проектами**: Создание, просмотр, выбор
- ✅ **Управление флагами**: CRUD операции
- ✅ **Управление экспериментами**: Просмотр и создание
- ✅ **API ключи**: Управление ключами

## Платформы

- **Android** - Native Android приложение
- **iOS** - Native iOS приложение
- **Desktop (JVM)** - Desktop приложение
- **Web (JS)** - Веб-приложение в браузере

## Использование

### Android

```kotlin
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.maxluxs.flagship.admin.ui.compose.AdminApp

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        AdminApp(apiBaseUrl = "http://localhost:8080")
    }
}
```

### iOS

```swift
import SwiftUI
import FlagshipAdminUI

@main
struct AdminApp: App {
    var body: some Scene {
        WindowGroup {
            AdminAppView(apiBaseUrl: "http://localhost:8080")
        }
    }
}
```

### Web

```kotlin
import androidx.compose.web.renderComposable
import io.maxluxs.flagship.admin.ui.compose.AdminApp

fun main() {
    renderComposable(rootElementId = "root") {
        AdminApp(apiBaseUrl = "http://localhost:8080")
    }
}
```

## Архитектура

```
AdminApp
├── Navigation
│   ├── LoginScreen
│   ├── RegisterScreen
│   ├── DashboardScreen
│   └── ProjectDetailScreen
│       ├── FlagsScreen
│       ├── ExperimentsScreen
│       └── ApiKeysScreen
├── API Client
│   └── AdminApiClient (expect/actual для платформ)
└── Theme
    └── AdminTheme (Material 3)
```

## TODO

- [ ] Редактирование флагов
- [ ] Toggle флагов (on/off)
- [ ] Полная форма создания эксперимента
- [ ] Удаление API ключей
- [ ] Настройки проекта
- [ ] Аналитика
- [ ] Audit log просмотр

