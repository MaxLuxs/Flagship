# Flagship Node.js SDK

Node.js SDK for Flagship feature flags.

## Installation

```bash
npm install @flagship/nodejs-sdk
# or
yarn add @flagship/nodejs-sdk
```

## Usage

### Basic Usage

```typescript
import { Flagship } from '@flagship/nodejs-sdk';

const flagship = new Flagship({
  apiKey: 'your-api-key',
  baseUrl: 'https://api.example.com/flags',
  environment: 'production'
});

await flagship.init();

// Check flag
if (flagship.isEnabled('new_ui')) {
  console.log('New UI enabled');
}

// Get typed value
const timeout: number = flagship.get('api_timeout', 5000);

// Get experiment
const assignment = flagship.experiment('checkout_flow');
if (assignment?.variant === 'A') {
  console.log('Using variant A');
}
```

### Singleton Pattern

```typescript
import { init, isEnabled, get, experiment } from '@flagship/nodejs-sdk';

// Initialize once
await init({
  apiKey: 'your-api-key',
  baseUrl: 'https://api.example.com/flags'
});

// Use anywhere
if (await isEnabled('new_ui')) {
  // ...
}

const timeout = await get('api_timeout', 5000);
const variant = await experiment('checkout_flow')?.variant;
```

### Express Middleware

```typescript
import express from 'express';
import { Flagship } from '@flagship/nodejs-sdk';

const app = express();
const flagship = new Flagship({
  apiKey: process.env.FLAGSHIP_API_KEY!,
  baseUrl: process.env.FLAGSHIP_BASE_URL!
});

await flagship.init();

app.get('/checkout', async (req, res) => {
  const variant = flagship.experiment('checkout_flow')?.variant;
  
  switch (variant) {
    case 'control':
      res.send('Legacy checkout');
      break;
    case 'A':
      res.send('New checkout A');
      break;
    default:
      res.send('Default checkout');
  }
});
```

### Custom Cache

```typescript
import { Flagship, FlagsCache } from '@flagship/nodejs-sdk';
import Redis from 'ioredis';

class RedisCache implements FlagsCache {
  private redis: Redis;
  
  constructor() {
    this.redis = new Redis();
  }
  
  async get(key: string): Promise<string | null> {
    return await this.redis.get(key);
  }
  
  async set(key: string, value: string): Promise<void> {
    await this.redis.set(key, value);
  }
  
  async clear(): Promise<void> {
    await this.redis.flushdb();
  }
}

const flagship = new Flagship({
  apiKey: 'your-api-key',
  cache: new RedisCache()
});
```

## API

### `Flagship.isEnabled(key: string, defaultValue?: boolean): boolean`

Check if a boolean flag is enabled.

### `Flagship.get<T>(key: string, defaultValue: T): T`

Get a typed flag value.

### `Flagship.experiment(key: string): ExperimentAssignment | null`

Get experiment assignment.

### `Flagship.refresh(): Promise<void>`

Manually refresh flags from server.

