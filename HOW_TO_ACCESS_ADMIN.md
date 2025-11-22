# Как зайти в админскую панель

## Быстрый старт

### 1. Запустить сервер

**Вариант A: Через Gradle (для разработки)**
```bash
cd /Users/maxluxs/pet/Flagship
./gradlew :flagship-server:run
```

**Вариант B: Через Docker Compose**
```bash
cd /Users/maxluxs/pet/Flagship
docker-compose up -d
```

**Вариант C: Ручной запуск PostgreSQL + Gradle**
```bash
# 1. Запустить PostgreSQL (если не запущен)
docker run -d \
  --name flagship-postgres \
  -e POSTGRES_DB=flagship \
  -e POSTGRES_USER=flagship \
  -e POSTGRES_PASSWORD=flagship_dev_password \
  -p 5432:5432 \
  postgres:16-alpine

# 2. Запустить сервер
cd /Users/maxluxs/pet/Flagship
./gradlew :flagship-server:run
```

### 2. Открыть админку в браузере

```
http://localhost:8080/admin
```

Или просто:
```
http://localhost:8080/
```
(автоматически перенаправит на `/admin/`)

### 3. Зарегистрироваться или войти

**Регистрация:**
1. На странице логина нажмите "Регистрация"
2. Заполните:
   - Email (например: `admin@test.com`)
   - Password (например: `admin123`)
   - Имя (опционально)
3. Нажмите "Зарегистрироваться"

**Вход:**
1. Введите email и password
2. Нажмите "Войти"

После входа вы попадете на Dashboard с проектами.

---

## Что делать дальше

1. **Создать проект**
   - Нажмите "+ Создать проект"
   - Заполните название и slug
   - Нажмите "Создать"

2. **Выбрать проект**
   - Кликните на проект в списке слева
   - Откроется интерфейс управления флагами

3. **Создать флаг**
   - Нажмите "+ Флаг"
   - Заполните ключ, тип, значение
   - Нажмите "Создать"

4. **Создать API ключ**
   - Перейдите во вкладку "Настройки"
   - Нажмите "+ Создать API ключ"
   - Сохраните ключ (он показывается только один раз!)

---

## Проверка, что сервер запущен

```bash
# Проверить, что сервер отвечает
curl http://localhost:8080/admin/

# Или проверить API
curl http://localhost:8080/api/auth/login
```

Если сервер не запущен, вы увидите ошибку подключения.

---

## Возможные проблемы

### Сервер не запускается

**Ошибка: "Port 8080 is already in use"**
```bash
# Найти процесс на порту 8080
lsof -i :8080

# Остановить процесс
kill -9 <PID>
```

**Ошибка: "Database connection failed"**
- Убедитесь, что PostgreSQL запущен
- Проверьте переменные окружения:
  - `DATABASE_URL` (по умолчанию: `jdbc:postgresql://localhost:5432/flagship`)
  - `DATABASE_USER` (по умолчанию: `flagship`)
  - `DATABASE_PASSWORD` (по умолчанию: `flagship_dev_password`)

### Админка не открывается

**404 Not Found**
- Убедитесь, что сервер запущен
- Проверьте URL: `http://localhost:8080/admin/` (слеш в конце важен)

**CORS ошибки**
- Сервер настроен на `anyHost()` в CORS, должно работать
- Если проблемы, проверьте консоль браузера

### Не могу зарегистрироваться/войти

**Ошибка: "Ошибка регистрации"**
- Проверьте консоль браузера (F12) для деталей
- Проверьте логи сервера
- Убедитесь, что база данных создана и доступна

---

## API Base URL

По умолчанию админка использует:
```javascript
const API_BASE = 'http://localhost:8080';
```

Если сервер запущен на другом порту или хосте, нужно изменить это в:
```
flagship-server/src/main/resources/admin-ui/app.js
```

---

## Логи сервера

При запуске через Gradle логи выводятся в консоль. Ищите:
- `Application started` - сервер запущен
- `Database connected` - база данных подключена
- Ошибки подключения к БД

---

## Остановка сервера

**Gradle:**
- Нажмите `Ctrl+C` в терминале

**Docker Compose:**
```bash
docker-compose down
```

---

## Полезные ссылки

- **Админка**: http://localhost:8080/admin
- **API Docs**: см. `flagship-server/README.md`
- **Health Check**: http://localhost:8080/api/auth/login (должен вернуть ошибку метода, но не 404)

