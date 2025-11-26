# Настройка внутренних модулей через Git Submodule

## Вариант 1: Git Submodule (Рекомендуется)

### Шаг 1: Создать приватный репозиторий

1. Создайте новый приватный репозиторий на GitHub: `Flagship-Internal`
2. Склонируйте его локально:
   ```bash
   git clone git@github.com:MaxLuxs/Flagship-Internal.git /tmp/flagship-internal
   ```

### Шаг 2: Переместить модули в приватный репозиторий

```bash
# Перейти в приватный репозиторий
cd /tmp/flagship-internal

# Скопировать модули из публичного репозитория
cp -r /Users/maxluxs/pet/Flagship/flagship-admin-ui-compose .
cp -r /Users/maxluxs/pet/Flagship/flagship-server .
cp -r /Users/maxluxs/pet/Flagship/flagship-shared .

# Закоммитить и запушить
git add .
git commit -m "Initial commit: internal modules"
git push origin main
```

### Шаг 3: Добавить submodule в публичный репозиторий

```bash
# Вернуться в публичный репозиторий
cd /Users/maxluxs/pet/Flagship

# Удалить локальные директории (они будут заменены submodule)
rm -rf flagship-admin-ui-compose flagship-server flagship-shared

# Добавить submodule
git submodule add git@github.com:MaxLuxs/Flagship-Internal.git internal

# Или добавить каждый модуль отдельно:
# git submodule add git@github.com:MaxLuxs/Flagship-Internal.git flagship-admin-ui-compose
# git submodule add git@github.com:MaxLuxs/Flagship-Internal.git flagship-server
# git submodule add git@github.com:MaxLuxs/Flagship-Internal.git flagship-shared

# Закоммитить
git add .gitmodules internal
git commit -m "Add internal modules as submodule"
git push
```

### Шаг 4: Обновить settings.gradle.kts

```kotlin
// Добавить обратно в settings.gradle.kts
include(":flagship-server")
include(":flagship-admin-ui-compose")
include(":flagship-shared")

// Или если submodule в папке internal:
project(":flagship-server").projectDir = file("internal/flagship-server")
project(":flagship-admin-ui-compose").projectDir = file("internal/flagship-admin-ui-compose")
project(":flagship-shared").projectDir = file("internal/flagship-shared")
```

### Работа с submodule

```bash
# Клонировать репозиторий с submodule
git clone --recursive git@github.com:MaxLuxs/Flagship.git

# Или если уже склонирован без submodule
git submodule update --init --recursive

# Обновить submodule до последней версии
cd internal
git pull origin main
cd ..
git add internal
git commit -m "Update internal modules"
git push

# Работать с кодом в submodule
cd internal
# Делать изменения, коммитить, пушить
git add .
git commit -m "Update internal modules"
git push origin main
cd ..
git add internal
git commit -m "Update submodule reference"
git push
```

---

## Вариант 2: Отдельный приватный репозиторий (без submodule)

### Шаг 1: Создать приватный репозиторий

1. Создайте `Flagship-Internal` на GitHub
2. Склонируйте его в отдельную директорию

### Шаг 2: Синхронизация через скрипт

Создайте скрипт `scripts/sync-internal.sh`:

```bash
#!/bin/bash
# Синхронизация внутренних модулей между репозиториями

INTERNAL_REPO="/path/to/Flagship-Internal"
PUBLIC_REPO="/Users/maxluxs/pet/Flagship"

# Копировать из публичного в приватный (если изменения в публичном)
cp -r "$PUBLIC_REPO/flagship-admin-ui-compose" "$INTERNAL_REPO/"
cp -r "$PUBLIC_REPO/flagship-server" "$INTERNAL_REPO/"
cp -r "$PUBLIC_REPO/flagship-shared" "$INTERNAL_REPO/"

# Или наоборот - из приватного в публичный (для локальной разработки)
# cp -r "$INTERNAL_REPO/flagship-admin-ui-compose" "$PUBLIC_REPO/"
# cp -r "$INTERNAL_REPO/flagship-server" "$PUBLIC_REPO/"
# cp -r "$INTERNAL_REPO/flagship-shared" "$PUBLIC_REPO/"
```

**Минусы:** Ручная синхронизация, легко забыть обновить.

---

## Вариант 3: Оставить текущий подход

Текущий подход с pre-push hook работает, но имеет ограничения:
- ✅ Код остается локально
- ✅ Не пушится в публичный репозиторий
- ❌ Нет версионирования внутренних модулей
- ❌ Неудобно для команды
- ❌ Риск случайного push

---

## Рекомендация

**Используйте Git Submodule (Вариант 1)** - это самый профессиональный и удобный способ для работы с приватным кодом в публичном репозитории.

### Преимущества submodule:
- ✅ Версионирование внутренних модулей
- ✅ Автоматическая синхронизация
- ✅ Команда может работать с приватным кодом
- ✅ Чистое разделение публичного и приватного
- ✅ Легко обновлять и синхронизировать

### Недостатки:
- ⚠️ Нужно обновлять submodule отдельно
- ⚠️ Новичкам может быть неочевидно (но есть документация)
