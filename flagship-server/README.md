# Flagship Server

Production-ready server for Flagship feature flags and experiments management.

## Features

- ✅ Multi-tenant architecture (projects)
- ✅ JWT authentication
- ✅ PostgreSQL database with Exposed ORM
- ✅ REST API for flags and experiments
- ✅ Admin API for project management
- ✅ API key management
- ✅ Docker support
- ✅ Audit logging

## Quick Start

### Using Docker Compose

```bash
docker-compose up -d
```

Server will start on `http://localhost:8080`

### Manual Setup

1. Start PostgreSQL:
```bash
docker run -d \
  --name flagship-postgres \
  -e POSTGRES_DB=flagship \
  -e POSTGRES_USER=flagship \
  -e POSTGRES_PASSWORD=flagship_dev_password \
  -p 5432:5432 \
  postgres:16-alpine
```

2. Set environment variables:
```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/flagship
export DATABASE_USER=flagship
export DATABASE_PASSWORD=flagship_dev_password
export JWT_SECRET=your-secret-key-here
```

3. Run the server:
```bash
./gradlew :flagship-server:run
```

## API Endpoints

### Authentication

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT token

### Admin API

- `GET /api/admin/projects` - List user's projects
- `POST /api/admin/projects` - Create new project
- `GET /api/admin/projects/{projectId}` - Get project details
- `GET /api/admin/projects/{projectId}/api-keys` - List API keys
- `POST /api/admin/projects/{projectId}/api-keys` - Create API key

### Project API

- `GET /api/projects/{projectId}/flags` - List flags
- `POST /api/projects/{projectId}/flags` - Create flag
- `PUT /api/projects/{projectId}/flags/{key}` - Update flag
- `DELETE /api/projects/{projectId}/flags/{key}` - Delete flag
- `GET /api/projects/{projectId}/experiments` - List experiments
- `POST /api/projects/{projectId}/experiments` - Create experiment
- `GET /api/projects/{projectId}/config` - Get config for SDK

## Environment Variables

- `DATABASE_URL` - PostgreSQL connection URL
- `DATABASE_USER` - Database user
- `DATABASE_PASSWORD` - Database password
- `JWT_SECRET` - Secret key for JWT tokens (required in production)
- `SERVER_PORT` - Server port (default: 8080)
- `SERVER_HOST` - Server host (default: 0.0.0.0)

## Production Deployment

1. Set strong `JWT_SECRET`
2. Use environment variables for database credentials
3. Enable HTTPS
4. Configure CORS for your domain
5. Set up database backups
6. Monitor logs

## Docker Build

```bash
docker build -t flagship-server -f flagship-server/Dockerfile .
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/flagship \
  -e JWT_SECRET=your-secret \
  flagship-server
```
