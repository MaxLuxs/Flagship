# Развертывание Flagship Server

Этот документ описывает различные способы развертывания Flagship Server на популярных платформах.

## Варианты развертывания

### 1. Railway (Рекомендуется для быстрого старта)

Railway - одна из самых простых платформ для развертывания приложений с PostgreSQL.

#### Преимущества:
- ✅ Автоматическое развертывание из GitHub
- ✅ Встроенная поддержка PostgreSQL
- ✅ Бесплатный tier для начала
- ✅ Простая настройка переменных окружения
- ✅ Автоматические SSL сертификаты

#### Шаги развертывания:

1. **Создайте аккаунт на Railway:**
   - Перейдите на https://railway.app
   - Войдите через GitHub

2. **Создайте новый проект:**
   - Нажмите "New Project"
   - Выберите "Deploy from GitHub repo"
   - Выберите ваш репозиторий Flagship

3. **Добавьте PostgreSQL:**
   - В проекте нажмите "+ New"
   - Выберите "Database" → "PostgreSQL"
   - Railway автоматически создаст базу данных

4. **Добавьте сервис для сервера:**
   - Нажмите "+ New" → "GitHub Repo"
   - Выберите ваш репозиторий
   - Railway автоматически определит Dockerfile

5. **Настройте переменные окружения:**
   - В настройках сервиса перейдите в "Variables"
   - Добавьте следующие переменные:

   ```bash
   DATABASE_URL=${{Postgres.DATABASE_URL}}
   DATABASE_USER=${{Postgres.USER}}
   DATABASE_PASSWORD=${{Postgres.PASSWORD}}
   JWT_SECRET=<сгенерируйте секретный ключ>
   SERVER_PORT=8080
   SERVER_HOST=0.0.0.0
   LOG_LEVEL=INFO
   ```

   **Важно:** Для `JWT_SECRET` сгенерируйте безопасный ключ:
   ```bash
   openssl rand -base64 32
   ```

6. **Настройте автоматический деплой:**
   - Railway автоматически развернет приложение при push в main ветку
   - Или используйте GitHub Actions workflow (см. `.github/workflows/deploy.yml`)

7. **Получите URL:**
   - Railway автоматически назначит домен
   - Или настройте кастомный домен в настройках

#### Использование GitHub Actions:

Добавьте в GitHub Secrets:
- `RAILWAY_TOKEN` - токен API Railway (найдите в настройках аккаунта)

---

### 2. Render

Render предоставляет бесплатный tier и простую интеграцию с GitHub.

#### Преимущества:
- ✅ Бесплатный tier (с ограничениями)
- ✅ Автоматическое развертывание из GitHub
- ✅ Встроенная поддержка PostgreSQL
- ✅ Автоматические SSL сертификаты

#### Шаги развертывания:

1. **Создайте аккаунт на Render:**
   - Перейдите на https://render.com
   - Войдите через GitHub

2. **Создайте PostgreSQL базу данных:**
   - Нажмите "New +" → "PostgreSQL"
   - Выберите бесплатный план (Free)
   - Запишите connection string

3. **Создайте Web Service:**
   - Нажмите "New +" → "Web Service"
   - Подключите ваш GitHub репозиторий
   - Настройки:
     - **Name:** `flagship-server`
     - **Environment:** `Docker`
     - **Dockerfile Path:** `flagship-server/Dockerfile`
     - **Docker Context:** `/` (корень репозитория)
     - **Build Command:** (оставьте пустым)
     - **Start Command:** (оставьте пустым, используется из Dockerfile)

4. **Настройте переменные окружения:**
   - В разделе "Environment" добавьте:

   ```bash
   DATABASE_URL=<connection string из PostgreSQL>
   DATABASE_USER=<user из PostgreSQL>
   DATABASE_PASSWORD=<password из PostgreSQL>
   JWT_SECRET=<сгенерируйте секретный ключ>
   SERVER_PORT=8080
   SERVER_HOST=0.0.0.0
   LOG_LEVEL=INFO
   ```

5. **Настройте автоматический деплой:**
   - Render автоматически развернет при push в main ветку
   - Или используйте GitHub Actions с deploy hook

#### Использование GitHub Actions:

1. В Render создайте Deploy Hook:
   - Перейдите в настройки сервиса
   - Найдите "Manual Deploy Hook"
   - Скопируйте URL

2. Добавьте в GitHub Secrets:
   - `RENDER_DEPLOY_HOOK_URL` - URL deploy hook

---

### 3. Fly.io

Fly.io отлично подходит для Docker контейнеров и глобального развертывания.

#### Преимущества:
- ✅ Глобальное развертывание (близко к пользователям)
- ✅ Отличная поддержка Docker
- ✅ Гибкая конфигурация
- ✅ Бесплатный tier для начала

#### Шаги развертывания:

1. **Установите Fly CLI:**
   ```bash
   curl -L https://fly.io/install.sh | sh
   ```

2. **Войдите в Fly.io:**
   ```bash
   fly auth login
   ```

3. **Создайте приложение:**
   ```bash
   cd flagship-server
   fly launch
   ```
   
   Fly CLI спросит:
   - Имя приложения (или оставьте автоматическое)
   - Регион развертывания
   - PostgreSQL базу данных (создайте новую)

4. **Настройте fly.toml:**
   Создайте файл `fly.toml` в корне проекта:

   ```toml
   app = "flagship-server"
   primary_region = "iad"  # Ваш регион

   [build]
     dockerfile = "flagship-server/Dockerfile"

   [env]
     SERVER_PORT = "8080"
     SERVER_HOST = "0.0.0.0"
     LOG_LEVEL = "INFO"

   [[services]]
     internal_port = 8080
     protocol = "tcp"

     [[services.ports]]
       handlers = ["http"]
       port = 80

     [[services.ports]]
       handlers = ["tls", "http"]
       port = 443

     [services.concurrency]
       type = "connections"
       hard_limit = 1000
       soft_limit = 500

     [[services.http_checks]]
       interval = "10s"
       timeout = "2s"
       grace_period = "5s"
       method = "GET"
       path = "/health"
       protocol = "http"
   ```

5. **Настройте секреты:**
   ```bash
   fly secrets set JWT_SECRET="<сгенерируйте секретный ключ>"
   fly secrets set DATABASE_URL="<connection string>"
   fly secrets set DATABASE_USER="<user>"
   fly secrets set DATABASE_PASSWORD="<password>"
   ```

6. **Разверните:**
   ```bash
   fly deploy
   ```

#### Использование GitHub Actions:

Добавьте в GitHub Secrets:
- `FLY_API_TOKEN` - токен API Fly.io (получите через `fly auth token`)

---

### 4. DigitalOcean App Platform

DigitalOcean App Platform предоставляет простой способ развертывания с автоматическим масштабированием.

#### Преимущества:
- ✅ Простая интеграция с GitHub
- ✅ Автоматическое масштабирование
- ✅ Встроенная поддержка PostgreSQL
- ✅ Автоматические SSL сертификаты

#### Шаги развертывания:

1. **Создайте аккаунт на DigitalOcean:**
   - Перейдите на https://cloud.digitalocean.com
   - Создайте аккаунт

2. **Создайте App:**
   - Перейдите в "App Platform"
   - Нажмите "Create App"
   - Подключите GitHub репозиторий

3. **Настройте компоненты:**
   - **Web Service:**
     - **Source:** GitHub репозиторий
     - **Build Command:** `./gradlew :flagship-server:build --no-daemon`
     - **Run Command:** `java -jar flagship-server/build/libs/*.jar`
     - **HTTP Port:** `8080`
     - **Health Check:** `/health`

   - **Database:**
     - Выберите "PostgreSQL"
     - Выберите план (Basic для начала)

4. **Настройте переменные окружения:**
   - В настройках Web Service добавьте:

   ```bash
   DATABASE_URL=${db.DATABASE_URL}
   DATABASE_USER=${db.USERNAME}
   DATABASE_PASSWORD=${db.PASSWORD}
   JWT_SECRET=<сгенерируйте секретный ключ>
   SERVER_PORT=8080
   SERVER_HOST=0.0.0.0
   LOG_LEVEL=INFO
   ```

5. **Разверните:**
   - DigitalOcean автоматически развернет приложение
   - Или используйте GitHub Actions

---

### 5. Heroku

Heroku - классический выбор для быстрого развертывания.

#### Преимущества:
- ✅ Очень простая настройка
- ✅ Множество аддонов (включая PostgreSQL)
- ✅ Автоматическое развертывание из GitHub

#### Шаги развертывания:

1. **Установите Heroku CLI:**
   ```bash
   # macOS
   brew tap heroku/brew && brew install heroku
   
   # Linux
   curl https://cli-assets.heroku.com/install.sh | sh
   ```

2. **Войдите в Heroku:**
   ```bash
   heroku login
   ```

3. **Создайте приложение:**
   ```bash
   heroku create flagship-server
   ```

4. **Добавьте PostgreSQL:**
   ```bash
   heroku addons:create heroku-postgresql:mini
   ```

5. **Настройте переменные окружения:**
   ```bash
   heroku config:set JWT_SECRET="<сгенерируйте секретный ключ>"
   heroku config:set SERVER_PORT=8080
   heroku config:set SERVER_HOST=0.0.0.0
   heroku config:set LOG_LEVEL=INFO
   ```

   Heroku автоматически установит `DATABASE_URL` из аддона PostgreSQL.

6. **Создайте Procfile:**
   Создайте файл `Procfile` в корне проекта:
   ```
   web: java -jar flagship-server/build/libs/*.jar
   ```

7. **Разверните:**
   ```bash
   git push heroku main
   ```

   Или подключите GitHub для автоматического развертывания:
   - В настройках приложения на Heroku
   - Включите "Automatic deploys from GitHub"

---

## Общие рекомендации для всех платформ

### Генерация JWT_SECRET

Всегда используйте безопасный секретный ключ:

```bash
# Генерация 32-символьного ключа
openssl rand -base64 32

# Или более длинный (рекомендуется)
openssl rand -base64 64
```

### Настройка CORS

В продакшене ограничьте CORS origins:

```bash
CORS_ORIGINS=https://yourdomain.com,https://admin.yourdomain.com
```

### Настройка базы данных

Для продакшена:
- Используйте SSL соединения (`DB_SSL=true`)
- Настройте connection pooling
- Регулярно делайте бэкапы
- Используйте managed PostgreSQL (не в контейнере)

### Мониторинг

Настройте мониторинг для:
- Health checks (`/health`)
- Metrics (`/metrics`)
- Логирование ошибок
- Database connection pool

### Безопасность

- ✅ Никогда не коммитьте секреты в git
- ✅ Используйте разные секреты для разных окружений
- ✅ Регулярно ротируйте JWT_SECRET
- ✅ Используйте HTTPS везде
- ✅ Ограничьте CORS origins
- ✅ Настройте rate limiting (на уровне платформы или через reverse proxy)

---

## GitHub Actions для автоматического развертывания

Проект включает GitHub Actions workflow (`.github/workflows/deploy.yml`) для автоматического:
- Сборки и тестирования
- Создания Docker образа
- Публикации в GitHub Container Registry
- Развертывания на выбранную платформу

### Настройка секретов в GitHub

1. Перейдите в Settings → Secrets and variables → Actions
2. Добавьте необходимые секреты:
   - `RAILWAY_TOKEN` - для Railway
   - `RENDER_DEPLOY_HOOK_URL` - для Render
   - `FLY_API_TOKEN` - для Fly.io

### Использование

Workflow автоматически запускается при:
- Push в `main` или `develop` ветку
- Изменениях в файлах сервера
- Ручном запуске через "Run workflow"

---

## Сравнение платформ

| Платформа | Бесплатный tier | Простота | PostgreSQL | Авто-деплой | Рекомендация |
|-----------|----------------|----------|------------|-------------|--------------|
| Railway   | ✅ Да          | ⭐⭐⭐⭐⭐ | ✅ Встроен | ✅ Да       | ⭐⭐⭐⭐⭐ |
| Render    | ✅ Да (огранич.) | ⭐⭐⭐⭐ | ✅ Встроен | ✅ Да       | ⭐⭐⭐⭐ |
| Fly.io    | ✅ Да          | ⭐⭐⭐   | ✅ Аддон   | ✅ Да       | ⭐⭐⭐⭐ |
| DigitalOcean | ❌ Нет      | ⭐⭐⭐⭐ | ✅ Встроен | ✅ Да       | ⭐⭐⭐ |
| Heroku    | ❌ Нет (только trial) | ⭐⭐⭐⭐⭐ | ✅ Аддон | ✅ Да | ⭐⭐⭐ |

---

## Troubleshooting

### Проблема: База данных не подключается

**Решение:**
- Проверьте `DATABASE_URL` формат: `jdbc:postgresql://host:port/database`
- Убедитесь, что база данных доступна из сети
- Проверьте firewall правила
- Для продакшена включите SSL: `DB_SSL=true`

### Проблема: Сервер не запускается

**Решение:**
- Проверьте логи на платформе
- Убедитесь, что все переменные окружения установлены
- Проверьте, что порт правильный (обычно 8080)
- Проверьте health check endpoint: `/health`

### Проблема: JWT токены не работают

**Решение:**
- Убедитесь, что `JWT_SECRET` установлен и достаточно длинный (минимум 32 символа)
- Проверьте, что секрет одинаковый для всех инстансов (если их несколько)
- Проверьте формат токена в запросах

---

## Дополнительные ресурсы

- [Railway Documentation](https://docs.railway.app)
- [Render Documentation](https://render.com/docs)
- [Fly.io Documentation](https://fly.io/docs)
- [DigitalOcean App Platform](https://www.digitalocean.com/products/app-platform)
- [Heroku Documentation](https://devcenter.heroku.com)

