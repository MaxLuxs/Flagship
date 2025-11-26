# Flagship Node.js Sample Application

A sample Node.js/Express application demonstrating how to use the Flagship feature flags SDK.

## Overview

This sample application showcases:

- **Feature Flags** - Boolean, integer, double, and string flag types
- **A/B Testing** - Experiment assignment with deterministic bucketing
- **Mock Provider** - In-memory mock data provider (no real server required)
- **REST API** - Express endpoints for flags, experiments, and demo scenarios
- **TypeScript** - Full type safety with TypeScript

## Features

### Flags

The sample includes the following mock flags:

- `new_feature` (bool) - Controls new experimental features
- `dark_mode` (bool) - Enables dark mode UI
- `max_retries` (int) - Maximum retry attempts for API calls
- `api_timeout` (double) - API timeout in seconds
- `welcome_message` (string) - Welcome message displayed to users
- `payment_enabled` (bool) - Enables payment processing

### Experiments

The sample includes the following mock experiments:

- `test_experiment` - Simple A/B test with control and treatment variants
- `checkout_flow` - Multi-variant checkout flow experiment (control, variant_a, variant_b)

## Setup

### Prerequisites

- Node.js 18+ 
- npm or yarn

### Installation

1. **Install dependencies:**

```bash
npm install
```

2. **Build the project:**

```bash
npm run build
```

3. **Start the server:**

```bash
npm start
```

Or run in development mode with auto-reload:

```bash
npm run dev
```

The server will start on `http://localhost:3000` by default.

## API Endpoints

### Health Check

```bash
GET /health
```

Returns server health status.

### Flags

#### List All Flags

```bash
GET /api/flags
```

**Response:**
```json
{
  "success": true,
  "flags": [
    {
      "key": "new_feature",
      "type": "bool",
      "value": true
    },
    ...
  ],
  "count": 6
}
```

#### Get Flag Value

```bash
GET /api/flags/:key
```

**Example:**
```bash
curl http://localhost:3000/api/flags/new_feature
```

**Response:**
```json
{
  "success": true,
  "key": "new_feature",
  "type": "bool",
  "value": true
}
```

#### Check if Flag is Enabled

```bash
GET /api/flags/:key/enabled?default=false
```

**Example:**
```bash
curl http://localhost:3000/api/flags/new_feature/enabled
```

**Response:**
```json
{
  "success": true,
  "key": "new_feature",
  "enabled": true,
  "defaultValue": false
}
```

### Experiments

#### List All Experiments

```bash
GET /api/experiments
```

**Response:**
```json
{
  "success": true,
  "experiments": [
    {
      "key": "checkout_flow",
      "variants": [
        { "name": "control", "weight": 0.33 },
        { "name": "variant_a", "weight": 0.33 },
        { "name": "variant_b", "weight": 0.34 }
      ],
      "exposureType": "onAssign"
    }
  ],
  "count": 2
}
```

#### Get Experiment Assignment

```bash
GET /api/experiments/:key?userId=user123
```

**Example:**
```bash
curl http://localhost:3000/api/experiments/checkout_flow?userId=user123
```

**Response:**
```json
{
  "success": true,
  "assignment": {
    "key": "checkout_flow",
    "variant": "variant_a",
    "payload": {}
  },
  "userId": "user123",
  "experiment": {
    "key": "checkout_flow",
    "variants": [...],
    "targeting": {...}
  }
}
```

### Demo Endpoints

#### Checkout Flow Demo

```bash
GET /api/demo/checkout?userId=user123
```

Demonstrates how to use the `checkout_flow` experiment to return different checkout configurations.

**Response:**
```json
{
  "success": true,
  "userId": "user123",
  "variant": "variant_a",
  "checkoutFlow": {
    "name": "Streamlined Checkout",
    "steps": ["cart", "payment", "confirmation"],
    "features": {
      "expressCheckout": true,
      "guestCheckout": true,
      "saveForLater": false
    }
  }
}
```

#### Feature Configuration Demo

```bash
GET /api/demo/feature
```

Demonstrates how to use multiple flags to configure application features.

**Response:**
```json
{
  "success": true,
  "featureConfig": {
    "newFeature": {
      "enabled": true,
      "description": "New experimental feature",
      "endpoints": ["/api/v2/new-endpoint", "/api/v2/advanced-feature"]
    },
    "payment": {
      "enabled": true,
      "maxRetries": 3,
      "timeout": 30.0
    },
    "ui": {
      "welcomeMessage": "Welcome to Flagship Demo!",
      "theme": "light"
    }
  }
}
```

## Code Examples

### Using Flags in Your Code

```typescript
import { isFlagEnabled, getFlag } from './mock-data';

// Check if a feature is enabled
if (isFlagEnabled('new_feature')) {
  // Use new feature
}

// Get flag value
const maxRetries = getFlag('max_retries');
if (maxRetries && maxRetries.type === 'int') {
  const retries = maxRetries.value as number;
  // Use retries value
}
```

### Using Experiments in Your Code

```typescript
import { assignExperiment, getExperiment } from './mock-data';

// Assign user to experiment variant
const userId = 'user123';
const experiment = getExperiment('checkout_flow');

if (experiment) {
  const assignment = assignExperiment('checkout_flow', userId, experiment);
  
  if (assignment) {
    switch (assignment.variant) {
      case 'control':
        // Use standard checkout
        break;
      case 'variant_a':
        // Use streamlined checkout
        break;
      case 'variant_b':
        // Use enhanced checkout
        break;
    }
  }
}
```

## Project Structure

```
flagship-sample-nodejs/
├── package.json          # Dependencies and scripts
├── tsconfig.json         # TypeScript configuration
├── README.md            # This file
├── .gitignore           # Git ignore rules
└── src/
    ├── index.ts         # Express app entry point
    ├── mock-data.ts     # Mock flags/experiments data
    └── routes/
        ├── flags.ts     # Flag-related endpoints
        ├── experiments.ts # Experiment-related endpoints
        └── demo.ts      # Demo endpoints
```

## Integration with Flagship SDK

This sample uses a mock data provider for demonstration purposes. In a real application, you would integrate with the `@flagship/nodejs-sdk`:

```typescript
import { Flagship, init } from '@flagship/nodejs-sdk';

// Initialize Flagship
const flagship = await init({
  apiKey: 'your-api-key',
  baseUrl: 'https://api.example.com/flags'
});

// Use flags
if (flagship.isEnabled('new_feature')) {
  // Use new feature
}

// Use experiments
const assignment = await flagship.experiment('checkout_flow');
if (assignment) {
  console.log(`User assigned to variant: ${assignment.variant}`);
}
```

## Development

### Running in Development Mode

```bash
npm run dev
```

This uses `ts-node-dev` for hot-reload during development.

### Building

```bash
npm run build
```

Compiles TypeScript to JavaScript in the `dist/` directory.

### Watching for Changes

```bash
npm run watch
```

Watches for file changes and recompiles automatically.

## Testing

You can test the API endpoints using curl or any HTTP client:

```bash
# List all flags
curl http://localhost:3000/api/flags

# Get specific flag
curl http://localhost:3000/api/flags/new_feature

# Check if flag is enabled
curl http://localhost:3000/api/flags/new_feature/enabled

# Get experiment assignment
curl http://localhost:3000/api/experiments/checkout_flow?userId=user123

# Demo endpoints
curl http://localhost:3000/api/demo/checkout?userId=user123
curl http://localhost:3000/api/demo/feature
```

## Notes

- This sample uses **mock data** - no real backend server is required
- Experiment assignment is **deterministic** - same user ID always gets the same variant
- The mock provider matches the data structure from the Kotlin `MockFlagsProvider`
- In production, replace the mock provider with the real Flagship SDK

## License

MIT

