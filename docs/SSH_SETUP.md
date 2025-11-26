# Настройка SSH для доступа к приватному репозиторию

## Проверка существующих SSH ключей

```bash
# Проверить существующие ключи
ls -la ~/.ssh/*.pub

# Проверить подключение к GitHub
ssh -T git@github.com
```

Если видите сообщение типа "Hi MaxLuxs! You've successfully authenticated...", значит SSH уже настроен.

## Создание нового SSH ключа

### 1. Создать ключ

```bash
# Создать новый SSH ключ (замените email на ваш)
ssh-keygen -t ed25519 -C "your_email@example.com"

# Или если ed25519 не поддерживается:
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
```

**Важно:**
- Нажмите Enter для использования дефолтного пути (`~/.ssh/id_ed25519`)
- Введите пароль (или оставьте пустым)
- Пароль можно оставить пустым для удобства, но это менее безопасно

### 2. Запустить ssh-agent

```bash
# Запустить ssh-agent
eval "$(ssh-agent -s)"

# Добавить ключ в ssh-agent
ssh-add ~/.ssh/id_ed25519
# Или для RSA:
# ssh-add ~/.ssh/id_rsa
```

### 3. Скопировать публичный ключ

```bash
# Показать публичный ключ (скопируйте весь вывод)
cat ~/.ssh/id_ed25519.pub
# Или для RSA:
# cat ~/.ssh/id_rsa.pub
```

### 4. Добавить ключ в GitHub

1. Откройте https://github.com/settings/keys
2. Нажмите **"New SSH key"**
3. Введите название (например, "MacBook Pro")
4. Вставьте скопированный ключ в поле "Key"
5. Нажмите **"Add SSH key"**

### 5. Проверить подключение

```bash
ssh -T git@github.com
```

Должно появиться сообщение:
```
Hi MaxLuxs! You've successfully authenticated, but GitHub does not provide shell access.
```

## Настройка для работы с submodule

После настройки SSH, submodule будет работать автоматически:

```bash
# Клонировать с submodule
git clone --recursive git@github.com:MaxLuxs/Flagship.git

# Или инициализировать существующий submodule
git submodule update --init --recursive
```

## Troubleshooting

### Ошибка "Permission denied (publickey)"

1. Проверьте, что ключ добавлен в ssh-agent:
   ```bash
   ssh-add -l
   ```

2. Если ключа нет, добавьте:
   ```bash
   ssh-add ~/.ssh/id_ed25519
   ```

3. Проверьте, что ключ добавлен в GitHub:
   - Откройте https://github.com/settings/keys
   - Убедитесь, что ваш ключ там есть

### Ошибка "Host key verification failed"

```bash
# Удалить старый ключ хоста
ssh-keygen -R github.com

# Попробовать подключиться снова
ssh -T git@github.com
# Введите "yes" когда спросит
```

### Использовать другой ключ для GitHub

Если у вас несколько ключей, создайте `~/.ssh/config`:

```bash
# Создать/отредактировать конфиг
nano ~/.ssh/config
```

Добавить:
```
Host github.com
  HostName github.com
  User git
  IdentityFile ~/.ssh/id_ed25519
  IdentitiesOnly yes
```

### Автоматический запуск ssh-agent (macOS)

Добавьте в `~/.zshrc` или `~/.bash_profile`:

```bash
# Автоматический запуск ssh-agent
if [ -z "$SSH_AUTH_SOCK" ]; then
   eval "$(ssh-agent -s)"
   ssh-add ~/.ssh/id_ed25519 2>/dev/null
fi
```

## Проверка доступа к приватному репозиторию

```bash
# Попробовать клонировать приватный репозиторий
git clone git@github.com:MaxLuxs/Flagship-Internal.git /tmp/test-clone

# Если успешно - доступ есть
# Если ошибка - проверьте настройки SSH
```
