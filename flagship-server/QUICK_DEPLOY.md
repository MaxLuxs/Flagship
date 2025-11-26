# Быстрое развертывание на GitHub

## TL;DR

**Да, можно развернуть сервер на GitHub!** Но не напрямую - GitHub Pages не поддерживает серверные приложения. Используйте GitHub Actions для автоматического развертывания на внешние платформы.

## Самый быстрый способ (Railway - 5 минут)

1. **Создайте аккаунт:** https://railway.app (войдите через GitHub)

2. **Создайте проект:**
   - New Project → Deploy from GitHub repo
   - Выберите ваш репозиторий Flagship

3. **Добавьте PostgreSQL:**
   - + New → Database → PostgreSQL

4. **Добавьте сервис:**
   - + New → GitHub Repo → выберите репозиторий
   - Railway автоматически определит Dockerfile

5. **Настройте переменные:**
   ```
   DATABASE_URL=${{Postgres.DATABASE_URL}}
   DATABASE_USER=${{Postgres.USER}}
   DATABASE_PASSWORD=${{Postgres.PASSWORD}}
   JWT_SECRET=<сгенерируйте: openssl rand -base64 32>
   SERVER_PORT=8080
   SERVER_HOST=0.0.0.0
   ```

6. **Готово!** Railway автоматически развернет сервер.

## Автоматическое развертывание через GitHub Actions

Проект уже включает GitHub Actions workflow (`.github/workflows/deploy.yml`).

### Настройка:

1. **Добавьте секреты в GitHub:**
   - Settings → Secrets and variables → Actions
   - Добавьте `RAILWAY_TOKEN` (или токен для другой платформы)

2. **Push в main ветку:**
   - Workflow автоматически соберет Docker образ
   - Опубликует в GitHub Container Registry
   - Развернет на выбранную платформу

### Поддерживаемые платформы:

- ✅ **Railway** - самый простой (рекомендуется)
- ✅ **Render** - бесплатный tier
- ✅ **Fly.io** - глобальное развертывание
- ✅ **DigitalOcean** - для продакшена
- ✅ **Heroku** - классический выбор

## Подробная документация

См. [DEPLOYMENT.md](./DEPLOYMENT.md) для детальных инструкций по каждой платформе.

## Что делает GitHub Actions?

1. **Сборка и тестирование:**
   - Компиляция Kotlin кода
   - Запуск unit тестов
   - Проверка качества кода

2. **Создание Docker образа:**
   - Сборка оптимизированного образа
   - Публикация в GitHub Container Registry
   - Тегирование версий

3. **Развертывание:**
   - Автоматический деплой на выбранную платформу
   - Настройка переменных окружения
   - Health checks

## FAQ

**Q: Можно ли развернуть напрямую на GitHub?**  
A: Нет, GitHub Pages только для статики. Но GitHub Actions может автоматически развернуть на внешние платформы.

**Q: Это бесплатно?**  
A: Да, Railway, Render и Fly.io имеют бесплатные tiers. GitHub Actions тоже бесплатен для публичных репозиториев.

**Q: Нужна ли база данных?**  
A: Да, серверу нужен PostgreSQL. Все платформы предоставляют managed PostgreSQL.

**Q: Как обновить сервер?**  
A: Просто push в main ветку - GitHub Actions автоматически развернет новую версию.

