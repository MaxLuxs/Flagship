import express, { Express, Request, Response } from 'express';
import flagsRouter from './routes/flags';
import experimentsRouter from './routes/experiments';
import demoRouter from './routes/demo';

const app: Express = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Health check endpoint
app.get('/health', (req: Request, res: Response) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    service: 'flagship-sample-nodejs'
  });
});

// API routes
app.use('/api/flags', flagsRouter);
app.use('/api/experiments', experimentsRouter);
app.use('/api/demo', demoRouter);

// Root endpoint
app.get('/', (req: Request, res: Response) => {
  res.json({
    name: 'Flagship Node.js Sample Application',
    version: '1.0.0',
    description: 'Sample application demonstrating Flagship feature flags SDK',
    endpoints: {
      health: '/health',
      flags: {
        list: 'GET /api/flags',
        get: 'GET /api/flags/:key',
        enabled: 'GET /api/flags/:key/enabled'
      },
      experiments: {
        list: 'GET /api/experiments',
        assign: 'GET /api/experiments/:key?userId=user123'
      },
      demo: {
        checkout: 'GET /api/demo/checkout?userId=user123',
        feature: 'GET /api/demo/feature'
      }
    },
    examples: [
      'curl http://localhost:3000/api/flags',
      'curl http://localhost:3000/api/flags/new_feature',
      'curl http://localhost:3000/api/flags/new_feature/enabled',
      'curl http://localhost:3000/api/experiments/checkout_flow?userId=user123',
      'curl http://localhost:3000/api/demo/checkout?userId=user123',
      'curl http://localhost:3000/api/demo/feature'
    ]
  });
});

// Error handling middleware
app.use((err: Error, req: Request, res: Response, next: any) => {
  console.error('Error:', err);
  res.status(500).json({
    success: false,
    error: 'Internal server error',
    message: err.message
  });
});

// 404 handler
app.use((req: Request, res: Response) => {
  res.status(404).json({
    success: false,
    error: 'Not found',
    path: req.path
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`🚀 Flagship Node.js Sample Server running on http://localhost:${PORT}`);
  console.log(`📖 API Documentation available at http://localhost:${PORT}`);
  console.log(`🏥 Health check: http://localhost:${PORT}/health`);
});

