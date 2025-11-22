# Архитектура админ-панели Flagship

## Обзор

Админ-панель Flagship — это веб-приложение для управления feature flags и A/B тестами. Построена на vanilla JavaScript с Material 3 дизайном.

---

## Технический стек

### Frontend
- **HTML5** — структура
- **CSS3** — стилизация (Material 3)
- **Vanilla JavaScript** — логика (ES6+)
- **Fetch API** — HTTP запросы
- **LocalStorage** — хранение JWT токенов

### Backend
- **Ktor Server** — REST API
- **PostgreSQL** — база данных
- **Exposed ORM** — работа с БД
- **JWT** — аутентификация
- **BCrypt** — хеширование паролей

---

## Структура приложения

### Файловая структура

```
flagship-server/src/main/resources/admin-ui/
├── index.html          # Главная страница
├── app.js              # Основная логика
└── styles.css          # Стили (Material 3)
```

### Архитектура компонентов

```
┌─────────────────────────────────────┐
│         Login Screen                 │
│  - Email/Password form               │
│  - Register form                     │
└─────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│         Dashboard                   │
│  ┌──────────┐  ┌─────────────────┐│
│  │ Sidebar  │  │   Main Content   ││
│  │ Projects │  │   - Flags        ││
│  │ List     │  │   - Experiments ││
│  │          │  │   - Settings     ││
│  └──────────┘  └─────────────────┘│
└─────────────────────────────────────┘
```

---

## Компоненты UI

### 1. Login Screen
**Файл**: `index.html` (строки 12-29)

**Функции**:
- `handleLogin()` — вход пользователя
- `handleRegister()` — регистрация
- `showLogin()` — показать форму входа
- `handleLogout()` — выход

**API endpoints**:
- `POST /api/auth/login`
- `POST /api/auth/register`

### 2. Dashboard
**Файл**: `index.html` (строки 32-115)

**Компоненты**:
- **Header** — название, email пользователя, кнопка выхода
- **Sidebar** — список проектов, кнопка создания
- **Main Content** — вкладки: Флаги, Эксперименты, Настройки

### 3. Projects List
**Файл**: `app.js` (строки 109-134)

**Функции**:
- `loadProjects()` — загрузка списка проектов
- `renderProjects()` — отрисовка списка
- `selectProject()` — выбор проекта

**API**: `GET /api/admin/projects`

### 4. Flags Tab
**Файл**: `app.js` (строки 150-184)

**Функции**:
- `loadFlags()` — загрузка флагов
- `renderFlags()` — отрисовка таблицы
- `showCreateFlagModal()` — модальное окно создания
- `deleteFlag()` — удаление флага

**API**:
- `GET /api/projects/{id}/flags`
- `POST /api/projects/{id}/flags`
- `DELETE /api/projects/{id}/flags/{key}`

**TODO**: Добавить toggle, редактирование

### 5. Experiments Tab
**Файл**: `app.js` (строки 186-219)

**Функции**:
- `loadExperiments()` — загрузка экспериментов
- `renderExperiments()` — отрисовка таблицы
- `deleteExperiment()` — удаление

**API**:
- `GET /api/projects/{id}/experiments`
- `DELETE /api/projects/{id}/experiments/{key}`

**TODO**: Добавить создание, редактирование

### 6. Settings Tab
**Файл**: `app.js` (строки 221-254)

**Функции**:
- `loadApiKeys()` — загрузка API ключей
- `renderApiKeys()` — отрисовка таблицы
- `showCreateApiKeyModal()` — создание ключа

**API**:
- `GET /api/admin/projects/{id}/api-keys`
- `POST /api/admin/projects/{id}/api-keys`

**TODO**: Добавить удаление ключей

### 7. Modals
**Файл**: `app.js` (строки 264-406)

**Модальные окна**:
- Создание проекта
- Создание флага
- Создание эксперимента (TODO)
- Создание API ключа

**Функции**:
- `showModal(content)` — показать модальное окно
- `closeModal()` — закрыть

---

## Состояние приложения

### Глобальные переменные

```javascript
const API_BASE = 'http://localhost:8080';
let token = localStorage.getItem('token');
let currentProjectId = null;
```

### LocalStorage

- `token` — JWT токен пользователя

---

## Поток данных

### Аутентификация

```
1. Пользователь вводит email/password
2. POST /api/auth/login
3. Сервер возвращает JWT токен
4. Токен сохраняется в localStorage
5. Переход на Dashboard
```

### Работа с проектами

```
1. Загрузка проектов: GET /api/admin/projects
2. Выбор проекта → установка currentProjectId
3. Загрузка данных проекта:
   - Флаги: GET /api/projects/{id}/flags
   - Эксперименты: GET /api/projects/{id}/experiments
   - API ключи: GET /api/admin/projects/{id}/api-keys
```

### Создание флага

```
1. Пользователь нажимает "+ Флаг"
2. Открывается модальное окно
3. Заполнение формы (key, type, value)
4. POST /api/projects/{id}/flags
5. Закрытие модального окна
6. Обновление списка флагов
```

---

## Обработка ошибок

### Текущая реализация

```javascript
try {
    const response = await fetch(...);
    if (response.ok) {
        // Успех
    } else {
        alert('Ошибка');
    }
} catch (error) {
    alert('Ошибка: ' + error.message);
}
```

### Улучшения (TODO)

- Показывать детальные сообщения об ошибках
- Валидация форм на клиенте
- Индикаторы загрузки
- Toast уведомления вместо alert

---

## Material 3 дизайн

### Цветовая схема

```css
/* Primary colors */
--md-sys-color-primary: #6750A4;
--md-sys-color-on-primary: #FFFFFF;

/* Surface colors */
--md-sys-color-surface: #FFFBFE;
--md-sys-color-surface-variant: #E7E0EC;

/* Error colors */
--md-sys-color-error: #BA1A1A;
```

### Компоненты

- **Cards** — для проектов, флагов
- **Buttons** — Filled, Outlined, Text
- **Text Fields** — для форм
- **Dialogs** — модальные окна
- **Data Tables** — списки флагов/экспериментов

---

## Безопасность

### Текущая реализация

- JWT токены в localStorage
- Авторизация через `Authorization: Bearer {token}`
- HTTPS (в продакшене)

### Улучшения (TODO)

- Refresh tokens
- Автоматическое обновление токенов
- Защита от XSS
- CSRF токены

---

## Производительность

### Оптимизации

- Ленивая загрузка данных (только при выборе проекта)
- Кэширование проектов в памяти
- Минимальные перерисовки DOM

### Улучшения (TODO)

- Виртуализация списков (для больших данных)
- Debounce для поиска
- Оптимистичные обновления UI

---

## Планы развития

### MVP (текущий этап)

- ✅ Login/Register
- ✅ Список проектов
- ✅ Создание проекта
- ✅ Список флагов
- ✅ Создание флага
- ✅ Удаление флага
- ✅ API ключи

### Phase 1 (ближайшее)

- [ ] Toggle флагов (on/off)
- [ ] Редактирование флага
- [ ] Создание эксперимента (полная форма)
- [ ] Редактирование эксперимента
- [ ] Удаление API ключа
- [ ] Улучшенная обработка ошибок

### Phase 2

- [ ] Realtime обновления (SSE/WebSocket)
- [ ] Аналитика (графики)
- [ ] Audit log просмотр
- [ ] Сегментация
- [ ] Поиск и фильтрация

### Phase 3

- [ ] Переход на React/Vue (опционально)
- [ ] PWA поддержка
- [ ] Оффлайн режим
- [ ] Темная тема

---

## API Reference

### Authentication

```javascript
POST /api/auth/login
Body: { email, password }
Response: { token, user: { id, email, name, isAdmin } }

POST /api/auth/register
Body: { email, password, name? }
Response: { token, user: { id, email, name, isAdmin } }
```

### Projects

```javascript
GET /api/admin/projects
Headers: { Authorization: Bearer {token} }
Response: Array<{ id, name, slug, description, ownerId, createdAt, updatedAt }>

POST /api/admin/projects
Headers: { Authorization: Bearer {token} }
Body: { name, slug, description? }
Response: { id, name, slug, description, ownerId, createdAt, updatedAt }
```

### Flags

```javascript
GET /api/projects/{projectId}/flags
Headers: { Authorization: Bearer {token} }
Response: { [key]: { type, value } }

POST /api/projects/{projectId}/flags
Headers: { Authorization: Bearer {token} }
Body: { [key]: { type, value } }
Response: { [key]: { type, value } }

DELETE /api/projects/{projectId}/flags/{key}
Headers: { Authorization: Bearer {token} }
```

### Experiments

```javascript
GET /api/projects/{projectId}/experiments
Headers: { Authorization: Bearer {token} }
Response: { [key]: { variants: [...], targeting: {...} } }
```

### API Keys

```javascript
GET /api/admin/projects/{projectId}/api-keys
Headers: { Authorization: Bearer {token} }
Response: Array<{ id, name, type, createdAt }>

POST /api/admin/projects/{projectId}/api-keys
Headers: { Authorization: Bearer {token} }
Body: { name, type: "READ_ONLY" | "ADMIN" }
Response: { id, name, key, type, createdAt }
```

---

## Тестирование

### Ручное тестирование

1. Регистрация нового пользователя
2. Создание проекта
3. Создание флага
4. Создание API ключа
5. Удаление флага

### Автоматическое тестирование (TODO)

- Unit тесты для функций
- E2E тесты (Playwright/Cypress)
- API тесты

---

## Документация для разработчиков

### Добавление нового компонента

1. Добавить HTML в `index.html`
2. Добавить стили в `styles.css`
3. Добавить логику в `app.js`
4. Добавить API endpoint в backend (если нужно)

### Стилизация

Использовать Material 3 токены:
- `--md-sys-color-primary`
- `--md-sys-color-surface`
- `--md-elevation-*`

### Обработка ошибок

Всегда показывать понятные сообщения:
```javascript
if (!response.ok) {
    const error = await response.json();
    alert(`Ошибка: ${error.error || response.statusText}`);
}
```

---

## Заключение

Админ-панель Flagship — это простое, но функциональное веб-приложение для управления feature flags. Построено на vanilla JavaScript для минимальных зависимостей и быстрой загрузки.

**Приоритеты**:
1. Исправить баги (создание проектов)
2. Добавить недостающий функционал (toggle, редактирование)
3. Улучшить UX (обработка ошибок, индикаторы)
4. Добавить аналитику и realtime

