# Настройка базы данных PostgreSQL

## Проблема

Ошибка при запуске сервера:
```
FATAL: role "flagship" does not exist
```

## Решение

### Вариант 1: Использовать существующую настройку (уже выполнено)

Пользователь и база данных уже созданы:
- **Пользователь**: `flagship`
- **Пароль**: `flagship_dev_password`
- **База данных**: `flagship`

### Вариант 2: Использовать текущего пользователя системы

Если хотите использовать текущего пользователя (например, `maxluxs`), измените переменные окружения:

```bash
export DATABASE_USER=maxluxs
export DATABASE_PASSWORD=
export DATABASE_URL=jdbc:postgresql://localhost:5432/flagship
```

### Вариант 3: Создать вручную

```bash
# Войти в PostgreSQL как суперпользователь
psql -U $USER -d postgres

# Создать пользователя
CREATE USER flagship WITH PASSWORD 'flagship_dev_password';

# Создать базу данных
CREATE DATABASE flagship OWNER flagship;

# Дать права
GRANT ALL PRIVILEGES ON DATABASE flagship TO flagship;

# Выйти
\q
```

## Проверка

```bash
# Проверить подключение
psql -U flagship -d flagship -c "SELECT version();"

# Проверить, что пользователь существует
psql -U $USER -d postgres -c "SELECT rolname FROM pg_roles WHERE rolname='flagship';"
```

## Переменные окружения

По умолчанию сервер использует:
- `DATABASE_URL`: `jdbc:postgresql://localhost:5432/flagship`
- `DATABASE_USER`: `flagship`
- `DATABASE_PASSWORD`: `flagship_dev_password`

Можно переопределить через переменные окружения:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/flagship
export DATABASE_USER=flagship
export DATABASE_PASSWORD=flagship_dev_password

./gradlew :flagship-server:run
```

## Docker Compose

Если используете Docker Compose, база данных создается автоматически:

```bash
docker-compose up -d
```

В `docker-compose.yml` уже настроено:
- Пользователь: `flagship`
- Пароль: `flagship_dev_password`
- База данных: `flagship`

## Устранение проблем

### Ошибка: "password authentication failed"

Проверьте пароль:
```bash
psql -U flagship -d flagship
# Введите пароль: flagship_dev_password
```

### Ошибка: "database does not exist"

Создайте базу данных:
```bash
psql -U $USER -d postgres -c "CREATE DATABASE flagship OWNER flagship;"
```

### Ошибка: "connection refused"

Убедитесь, что PostgreSQL запущен:
```bash
# macOS (Homebrew)
brew services start postgresql@14

# Или проверить статус
brew services list | grep postgresql
```

### Использовать другого пользователя

Если хотите использовать текущего пользователя системы:

1. Измените настройки в коде или через переменные окружения
2. Убедитесь, что у пользователя есть права на создание базы данных:
```bash
psql -U $USER -d postgres -c "ALTER USER $USER CREATEDB;"
```

