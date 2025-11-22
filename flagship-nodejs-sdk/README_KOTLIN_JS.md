# Использование Kotlin/JS в Node.js SDK

Node.js SDK использует скомпилированный Kotlin/JS код для ключевой логики, обеспечивая:

- **Единую логику bucketing** - MurmurHash3 алгоритм из Kotlin
- **Детерминистические назначения** - одинаковые результаты на всех платформах
- **Общий код** - без дублирования логики между Kotlin и TypeScript
- **Exponential backoff** - общая логика расчета задержек

## Как это работает

1. **Kotlin/JS компиляция**: `flagship-core` компилируется в JavaScript для Node.js
2. **TypeScript обертка**: Node.js SDK использует скомпилированный Kotlin код через TypeScript обертку (`kotlin-wrapper.ts`)
3. **Fallback**: Если Kotlin код недоступен, используется простая fallback реализация

## Настройка сборки

### 1. Компиляция Kotlin/JS

```bash
# В корне проекта
./gradlew :flagship-core:jsNodeProductionLibraryDistribution
```

Скомпилированные файлы будут в:
```
flagship-core/build/js/packages/flagship-core/kotlin/
```

### 2. Копирование в Node.js SDK

Скопируйте скомпилированные файлы в `flagship-nodejs-sdk/kotlin/`:

```bash
cp -r flagship-core/build/js/packages/flagship-core/kotlin flagship-nodejs-sdk/
```

### 3. Обновление package.json

Добавьте скрипт для автоматической сборки:

```json
{
  "scripts": {
    "build:kotlin": "cd ../flagship-core && ./gradlew jsNodeProductionLibraryDistribution && cd ../flagship-nodejs-sdk && cp -r ../flagship-core/build/js/packages/flagship-core/kotlin .",
    "build": "npm run build:kotlin && tsc",
    "prepublishOnly": "npm run build"
  }
}
```

## Использование

Node.js SDK автоматически использует Kotlin код для:

### 1. Experiment Assignment

```typescript
const assignment = await flagship.experiment('checkout_flow');
// Использует Kotlin BucketingEngine с MurmurHash3
```

### 2. Backoff Calculations

В будущем можно использовать для retry логики:

```typescript
import { loadKotlinCode } from './kotlin-wrapper';

const kotlin = await loadKotlinCode();
if (kotlin) {
  const delay = kotlin.calculateBackoff(attempt, 1000, 60000);
}
```

## Преимущества

1. **Консистентность**: Одинаковая логика на всех платформах (Android, iOS, Node.js)
2. **Тестируемость**: Логика тестируется один раз в Kotlin
3. **Поддержка**: Изменения в Kotlin автоматически применяются к Node.js SDK
4. **Производительность**: Скомпилированный Kotlin код оптимизирован

## Fallback

Если Kotlin код недоступен (например, при разработке или если сборка не выполнена), SDK автоматически использует простую fallback реализацию на TypeScript. Это обеспечивает работоспособность даже без Kotlin кода.

## Troubleshooting

### Kotlin код не загружается

1. Убедитесь, что выполнили `npm run build:kotlin`
2. Проверьте, что файлы в `flagship-nodejs-sdk/kotlin/` существуют
3. Проверьте импорты в `kotlin-wrapper.ts`

### TypeScript ошибки

Убедитесь, что в `tsconfig.json` включена поддержка ES modules и правильные пути для импорта.
