# Работа с Git Submodule для внутренних модулей

## Быстрый старт

### Первая настройка (после клонирования репозитория)

```bash
# Клонировать репозиторий с submodule
git clone --recursive git@github.com:MaxLuxs/Flagship.git

# Или если уже склонирован без submodule
git submodule update --init --recursive
```

### Обновление submodule

```bash
# Обновить submodule до последней версии
cd internal
git pull origin main
cd ..
git add internal
git commit -m "Update internal modules"
git push
```

## Работа с кодом в submodule

### Внести изменения в внутренние модули

```bash
# Перейти в submodule
cd internal

# Сделать изменения, закоммитить
git add .
git commit -m "Update internal modules"
git push origin main

# Вернуться в основной репозиторий
cd ..

# Обновить ссылку на submodule
git add internal
git commit -m "Update submodule reference"
git push
```

### Добавить новый файл в внутренний модуль

```bash
cd internal/flagship-server
# Создать/изменить файлы
git add .
git commit -m "Add new feature"
git push origin main
cd ../../..
git add internal
git commit -m "Update submodule"
git push
```

## Проверка статуса

```bash
# Проверить статус submodule
git submodule status

# Проверить, есть ли изменения в submodule
cd internal
git status
```

## Важные замечания

1. **Submodule инициализируется только если у вас есть доступ к приватному репозиторию**
   - Если submodule не инициализирован, модули просто не будут включены в сборку
   - Это нормально для публичного репозитория

2. **Всегда коммитьте обновления submodule в основном репозитории**
   - После изменений в `internal/`, не забудьте обновить ссылку в основном репозитории

3. **Для команды:**
   - Убедитесь, что у всех есть доступ к приватному репозиторию `Flagship-Internal`
   - Используйте `git clone --recursive` при первом клонировании

## Troubleshooting

### Submodule показывает "modified content"

```bash
# Это нормально, если вы внесли изменения в submodule
cd internal
git status
# Если нужно обновить ссылку в основном репозитории:
cd ..
git add internal
git commit -m "Update submodule"
```

### Submodule не инициализирован

```bash
git submodule update --init --recursive
```

### Ошибка доступа к приватному репозиторию

Убедитесь, что:
- У вас есть доступ к `Flagship-Internal` репозиторию
- SSH ключ настроен правильно
- Используется правильный URL (git@github.com, а не https://)
