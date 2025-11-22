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

#### Basic Express Integration

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

#### Express Middleware for Request Context

```typescript
import express, { Request, Response, NextFunction } from 'express';
import { Flagship } from '@flagship/nodejs-sdk';

const flagship = new Flagship({
  apiKey: process.env.FLAGSHIP_API_KEY!,
  baseUrl: process.env.FLAGSHIP_BASE_URL!
});

await flagship.init();

// Middleware to add Flagship to request
app.use((req: Request, res: Response, next: NextFunction) => {
  (req as any).flagship = flagship;
  next();
});

app.get('/api/feature', async (req: Request, res: Response) => {
  const flagship = (req as any).flagship as Flagship;
  
  if (flagship.isEnabled('new_api_endpoint')) {
    res.json({ message: 'New API endpoint' });
  } else {
    res.json({ message: 'Legacy endpoint' });
  }
});
```

### Next.js Integration

#### App Router (Next.js 13+)

```typescript
// app/api/checkout/route.ts
import { NextRequest, NextResponse } from 'next/server';
import { Flagship } from '@flagship/nodejs-sdk';

const flagship = new Flagship({
  apiKey: process.env.FLAGSHIP_API_KEY!,
  baseUrl: process.env.FLAGSHIP_BASE_URL!
});

// Initialize once (Next.js caches this)
let initialized = false;
if (!initialized) {
  await flagship.init();
  initialized = true;
}

export async function GET(request: NextRequest) {
  const variant = flagship.experiment('checkout_flow')?.variant;
  
  return NextResponse.json({ 
    variant,
    message: variant === 'A' ? 'New checkout' : 'Legacy checkout'
  });
}
```

#### Pages Router (Next.js 12)

```typescript
// pages/api/checkout.ts
import type { NextApiRequest, NextApiResponse } from 'next';
import { Flagship } from '@flagship/nodejs-sdk';

const flagship = new Flagship({
  apiKey: process.env.FLAGSHIP_API_KEY!,
  baseUrl: process.env.FLAGSHIP_BASE_URL!
});

let initialized = false;

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  if (!initialized) {
    await flagship.init();
    initialized = true;
  }
  
  const variant = flagship.experiment('checkout_flow')?.variant;
  res.status(200).json({ variant });
}
```

#### Next.js Server Component

```typescript
// app/checkout/page.tsx
import { Flagship } from '@flagship/nodejs-sdk';

const flagship = new Flagship({
  apiKey: process.env.FLAGSHIP_API_KEY!,
  baseUrl: process.env.FLAGSHIP_BASE_URL!
});

await flagship.init();

export default async function CheckoutPage() {
  const variant = flagship.experiment('checkout_flow')?.variant;
  
  return (
    <div>
      {variant === 'A' ? <NewCheckout /> : <LegacyCheckout />}
    </div>
  );
}
```

### NestJS Integration

#### Service Pattern

```typescript
// flagship.service.ts
import { Injectable, OnModuleInit } from '@nestjs/common';
import { Flagship } from '@flagship/nodejs-sdk';

@Injectable()
export class FlagshipService implements OnModuleInit {
  private flagship: Flagship;
  
  async onModuleInit() {
    this.flagship = new Flagship({
      apiKey: process.env.FLAGSHIP_API_KEY!,
      baseUrl: process.env.FLAGSHIP_BASE_URL!
    });
    await this.flagship.init();
  }
  
  isEnabled(key: string, defaultValue = false): boolean {
    return this.flagship.isEnabled(key, defaultValue);
  }
  
  get<T>(key: string, defaultValue: T): T {
    return this.flagship.get(key, defaultValue);
  }
  
  experiment(key: string) {
    return this.flagship.experiment(key);
  }
}
```

#### Controller Usage

```typescript
// checkout.controller.ts
import { Controller, Get } from '@nestjs/common';
import { FlagshipService } from './flagship.service';

@Controller('checkout')
export class CheckoutController {
  constructor(private readonly flagship: FlagshipService) {}
  
  @Get()
  async checkout() {
    const variant = this.flagship.experiment('checkout_flow')?.variant;
    
    switch (variant) {
      case 'A':
        return { flow: 'new-checkout-a' };
      case 'B':
        return { flow: 'new-checkout-b' };
      default:
        return { flow: 'legacy-checkout' };
    }
  }
}
```

#### NestJS Module

```typescript
// flagship.module.ts
import { Module, Global } from '@nestjs/common';
import { FlagshipService } from './flagship.service';

@Global()
@Module({
  providers: [FlagshipService],
  exports: [FlagshipService]
})
export class FlagshipModule {}
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

