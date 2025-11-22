# Flagship Admin Panel & Server

Коммерческий сервер и админ-панель для управления feature flags и экспериментами.

## Что реализовано

### ✅ Backend (Server)

1. **Docker & Infrastructure**
   - Dockerfile для сервера
   - docker-compose.yml с PostgreSQL
   - Настройка для продакшена

2. **Database**
   - PostgreSQL с Exposed ORM
   - Модели: User, Project, ProjectMember, ApiKey, Flag, Experiment, AuditLog
   - Автоматическое создание таблиц

3. **Authentication & Authorization**
   - JWT аутентификация
   - Регистрация и вход пользователей
   - Роли в проектах (OWNER, ADMIN, MEMBER, VIEWER)

4. **Multi-tenancy**
   - Проекты с изоляцией данных
   - Управление участниками проектов
   - API ключи на уровне проекта

5. **Admin API**
   - `/api/admin/projects` - управление проектами
   - `/api/admin/projects/{id}/api-keys` - управление API ключами
   - `/api/projects/{id}/flags` - CRUD флагов
   - `/api/projects/{id}/experiments` - CRUD экспериментов
   - `/api/projects/{id}/config` - получение конфига для SDK

### ✅ Frontend (Admin UI)

1. **Экраны MVP**
   - Login / Register
   - Dashboard с проектами
   - Список флагов с toggle
   - Список экспериментов
   - Настройки проекта (API ключи)

2. **Функциональность**
   - Создание/редактирование/удаление флагов
   - Создание/удаление экспериментов
   - Создание API ключей
   - Управление проектами

## Запуск

### Быстрый старт (Docker)

```bash
docker-compose up -d
```

Сервер: http://localhost:8080
Админка: http://localhost:8080/admin

### Ручной запуск

1. Запустить PostgreSQL
2. Установить переменные окружения (см. `.env.example`)
3. Запустить сервер:
```bash
./gradlew :flagship-server:run
```

## Использование

1. Открыть http://localhost:8080/admin
2. Зарегистрироваться
3. Создать проект
4. Создать флаги/эксперименты
5. Получить API ключ в настройках проекта
6. Использовать API ключ в SDK

## API Примеры

### Регистрация
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123","name":"User"}'
```

### Создание проекта
```bash
curl -X POST http://localhost:8080/api/admin/projects \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"My Project","slug":"my-project","description":"Description"}'
```

### Создание флага
```bash
curl -X POST http://localhost:8080/api/projects/PROJECT_ID/flags \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"new_feature":{"type":"bool","value":true}}'
```

### Получение конфига для SDK
```bash
curl http://localhost:8080/api/projects/PROJECT_ID/config?apiKey=YOUR_API_KEY
```

## Структура проекта

```
flagship-server/
├── src/main/kotlin/
│   ├── auth/          # JWT аутентификация
│   ├── admin/         # Admin API routes
│   ├── routes/        # Project API routes
│   ├── storage/       # Database storage
│   └── database/      # DB models & config
└── src/main/resources/
    └── admin-ui/      # Frontend админки
```

## Следующие шаги

- [ ] Аналитика и метрики
- [ ] Audit log запись
- [ ] Webhooks для интеграций
- [ ] SSE для real-time обновлений
- [ ] Расширенная сегментация
- [ ] A/B тестирование с метриками
- [ ] Экспорт/импорт конфигов

## Production Checklist

- [x] Docker setup
- [x] PostgreSQL database
- [x] JWT authentication
- [x] Multi-tenancy
- [x] Admin UI
- [ ] HTTPS/SSL
- [ ] Rate limiting
- [ ] Database backups
- [ ] Monitoring & logging
- [ ] CI/CD pipeline

