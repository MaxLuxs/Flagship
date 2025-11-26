# Настройка IDE для запуска Flagship Admin UI

## Проблемы

### Проблема 1: Несовместимость версий Java
При запуске через кнопку Run в IDE возникает ошибка:
```
class file version 65.0 (Java 21), but this version of Java Runtime only recognizes up to 61.0 (Java 17)
```

### Проблема 2: Отсутствующие зависимости в classpath
При запуске через кнопку Run возникает ошибка:
```
java.lang.NoClassDefFoundError: androidx/collection/MutableObjectList
```

Это происходит потому, что:
- IDE использует другую версию Java или неправильную конфигурацию запуска
- IDE не включает все транзитивные зависимости Compose Desktop в classpath
- Compose Desktop требует специальной настройки runtime classpath, которую Gradle делает автоматически

## Решение

### Вариант 1: Использовать Gradle задачу (рекомендуется)

Вместо запуска через кнопку Run, используйте Gradle задачу:

1. Откройте **Gradle** панель (View → Tool Windows → Gradle)
2. Найдите задачу `:flagship-admin-ui-compose:run`
3. Двойной клик для запуска

Или через терминал:
```bash
./gradlew :flagship-admin-ui-compose:run
```

### Вариант 2: Настроить Run Configuration в IDE

1. **Проверьте версию Java в IDE:**
   - File → Project Structure → Project → SDK
   - Убедитесь, что используется **Java 17** (или выше, но не ниже)

2. **Создайте правильную Run Configuration:**
   - Run → Edit Configurations...
   - Нажмите `+` → выберите **Gradle**
   - Настройки:
     - **Name**: `Flagship Admin UI`
     - **Gradle project**: `:flagship-admin-ui-compose`
     - **Tasks**: `run`
     - **JVM**: Убедитесь, что используется Java 17

3. **⚠️ НЕ используйте Kotlin/JVM конфигурацию напрямую!**
   
   Kotlin/JVM конфигурация не включает все транзитивные зависимости Compose Desktop (например, `androidx.collection`), что приводит к `NoClassDefFoundError`.
   
   Если все же нужно использовать Kotlin/JVM конфигурацию:
   - Run → Edit Configurations...
   - Нажмите `+` → выберите **Kotlin**
   - Настройки:
     - **Name**: `Flagship Admin UI`
     - **Main class**: `io.maxluxs.flagship.admin.ui.compose.MainKt`
     - **Use classpath of module**: `flagship-admin-ui-compose.jvmMain`
     - **JRE**: Выберите **Java 17**
     - **VM options**: Добавьте все JAR из runtime classpath (см. ниже)
   
   **Но это сложно и не рекомендуется!** Лучше использовать Gradle задачу.

### Вариант 3: Настроить Gradle JVM в IDE

1. File → Settings → Build, Execution, Deployment → Build Tools → Gradle
2. **Gradle JVM**: Выберите **Java 17** (или Project SDK, если он Java 17)

### Проверка версии Java

Убедитесь, что все используют Java 17:

```bash
# Проверка версии Java в системе
java -version

# Проверка версии Java, используемой Gradle
./gradlew --version
```

## Почему Gradle работает, а IDE нет?

### Проблема с версией Java:
- **Gradle** использует Java 17 из `JAVA_HOME` или настроенную в `gradle.properties`
- **IDE** может использовать другую версию Java из своих настроек

### Проблема с classpath (РЕШЕНО):
- **Gradle задача `run`** использует `jvmRuntimeClasspath`, который включает все runtime зависимости
- **IDE Kotlin/JVM конфигурация** использует `jvmCompileClasspath`, который НЕ включает runtime-only зависимости
- `androidx.collection:collection-jvm` была транзитивной зависимостью, которая присутствовала в `jvmRuntimeClasspath`, но отсутствовала в `jvmCompileClasspath`
- **Решение:** Добавлена явная зависимость `androidx.collection:collection-jvm:1.5.0` в `jvmMain.dependencies`, чтобы она попала в compile classpath

### Как получить полный classpath для IDE (если очень нужно):

```bash
./gradlew :flagship-admin-ui-compose:printClasspath
```

Это выведет все JAR файлы, которые нужно добавить в VM options как `-cp`.

## Решение проблемы

Проблема была решена добавлением явной зависимости `androidx.collection:collection-jvm:1.5.0` в `jvmMain.dependencies` в `build.gradle.kts`. Теперь эта зависимость присутствует как в compile, так и в runtime classpath, что позволяет IDE запускать приложение корректно.

### Что было сделано:

В файле `flagship-admin-ui-compose/build.gradle.kts` добавлено:
```kotlin
jvmMain.dependencies {
    // ... другие зависимости
    // Explicitly add androidx.collection for IDE run configuration
    implementation("androidx.collection:collection-jvm:1.5.0")
}
```

Теперь приложение должно запускаться как через Gradle задачу `run`, так и через кнопку Run в IDE.

### Альтернативный способ (если проблема сохраняется):

Если после добавления зависимости проблема все еще возникает, используйте Gradle задачу `run`:
1. В IDE: Gradle панель → `:flagship-admin-ui-compose` → Tasks → compose desktop → `run`
2. Или создайте Gradle Run Configuration с задачей `run`

### Проверка classpath:

```bash
# Runtime classpath (используется Gradle задачей run) - включает androidx.collection
./gradlew :flagship-admin-ui-compose:dependencies --configuration jvmRuntimeClasspath | grep collection

# Compile classpath (используется IDE) - НЕ включает androidx.collection
./gradlew :flagship-admin-ui-compose:dependencies --configuration jvmCompileClasspath | grep collection
```

