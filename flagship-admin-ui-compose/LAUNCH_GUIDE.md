# Flagship Admin UI - Launch Guide

Этот модуль предоставляет полноценное приложение Flagship Admin Panel для всех платформ.

## Платформы

### 🖥️ Desktop (JVM)

**Запуск из IDE:**
1. Откройте файл `src/jvmMain/kotlin/io/maxluxs/flagship/admin/ui/compose/Main.kt`
2. Правый клик → Run 'MainKt'

**Запуск из командной строки:**
```bash
./gradlew :flagship-admin-ui-compose:run
```

**Настройка API URL:**
```bash
# Через системное свойство
./gradlew :flagship-admin-ui-compose:run -Dflagship.api.url=http://localhost:8080

# Через переменную окружения
FLAGSHIP_API_URL=http://localhost:8080 ./gradlew :flagship-admin-ui-compose:run
```

### 📱 Android

**Запуск из IDE:**
1. Откройте файл `src/androidMain/kotlin/io/maxluxs/flagship/admin/ui/compose/MainActivity.kt`
2. Правый клик → Run 'MainActivity'

**Запуск из командной строки:**
```bash
./gradlew :flagship-admin-ui-compose:installDebug
adb shell am start -n io.maxluxs.flagship.admin.ui.compose/.MainActivity
```

**Настройка API URL:**
- **Эмулятор**: автоматически использует `http://10.0.2.2:8080` (это localhost хоста)
- **Физическое устройство**: использует `http://localhost:8080` (нужно настроить IP адрес сервера)

### 🌐 Web (JS)

**Запуск через сервер:**
```bash
# Сначала соберите JS
./gradlew :flagship-admin-ui-compose:jsBrowserDevelopmentWebpack

# Затем запустите сервер (который уже включает веб-приложение)
./gradlew :flagship-server:run
```

Откройте в браузере: `http://localhost:8080/admin/`

**Автоматическое определение API URL:**
Web версия автоматически определяет API URL из `window.location.origin`, поэтому работает с любым сервером.

### 🍎 iOS

**Настройка:**
1. Создайте iOS приложение (Xcode проект)
2. Добавьте зависимость на `flagship-admin-ui-compose` framework
3. Используйте `MainViewController()` в SwiftUI:

```swift
import SwiftUI
import FlagshipAdminUI

@main
struct AdminApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

**Настройка API URL:**
- **Симулятор**: использует `http://localhost:8080`
- **Физическое устройство**: нужно настроить IP адрес сервера

## Структура файлов

```
flagship-admin-ui-compose/
├── src/
│   ├── androidMain/
│   │   ├── MainActivity.kt          # Android точка входа
│   │   └── AndroidManifest.xml      # Android манифест
│   ├── iosMain/
│   │   └── MainViewController.kt    # iOS точка входа
│   ├── jvmMain/
│   │   └── Main.kt                   # Desktop точка входа
│   ├── jsMain/
│   │   ├── Main.kt                   # Web точка входа
│   │   └── resources/
│   │       └── index.html            # HTML шаблон
│   └── commonMain/
│       └── AdminApp.kt               # Общий UI код
```

## API URL по умолчанию

- **Desktop**: `http://localhost:8080`
- **Android Emulator**: `http://10.0.2.2:8080`
- **Android Device**: `http://localhost:8080` (нужно настроить)
- **iOS Simulator**: `http://localhost:8080`
- **iOS Device**: `http://localhost:8080` (нужно настроить)
- **Web**: автоматически из `window.location.origin`

## Примечания

- Все платформы используют один и тот же код UI из `commonMain`
- API URL можно настроить для каждой платформы отдельно
- Для production сборки настройте API URL через BuildConfig или конфигурационные файлы

