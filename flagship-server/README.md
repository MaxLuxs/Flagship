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

### Required Variables

- `DATABASE_URL` - PostgreSQL connection URL (e.g., `jdbc:postgresql://localhost:5432/flagship`)
- `DATABASE_USER` - Database user
- `DATABASE_PASSWORD` - Database password (use strong password in production!)
- `JWT_SECRET` - Secret key for JWT tokens (**REQUIRED in production**, min 32 characters)
  - Generate with: `openssl rand -base64 32`
  - **Never use default value in production!**

### Optional Variables

- `SERVER_PORT` - Server port (default: 8080)
- `SERVER_HOST` - Server host (default: 0.0.0.0)
- `LOG_LEVEL` - Logging level (default: INFO)
- `CORS_ORIGINS` - Comma-separated list of allowed CORS origins

### Configuration

1. **Copy `.env.example` to `.env`:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your actual values:**
   ```bash
   # Use strong passwords and secrets!
   DATABASE_PASSWORD=your-secure-password-here
   JWT_SECRET=$(openssl rand -base64 32)
   ```

3. **Load environment variables:**
   ```bash
   export $(cat .env | xargs)
   ```

**⚠️ Security Notes:**
- Never commit `.env` file to git (it's in `.gitignore`)
- Use different secrets for each environment (dev, staging, prod)
- Rotate secrets regularly in production
- Use Docker secrets or secret management tools (Vault, AWS Secrets Manager) for production

## Production Deployment

### Security Checklist

1. **✅ Set strong secrets:**
   ```bash
   # Generate secure JWT secret
   export JWT_SECRET=$(openssl rand -base64 32)
   
   # Use strong database password
   export DATABASE_PASSWORD=$(openssl rand -base64 24)
   ```

2. **✅ Use environment variables:**
   - Never hardcode secrets in code
   - Use `.env` file (not committed to git) or Docker secrets
   - Use different secrets for each environment

3. **✅ Enable HTTPS:**
   - Use reverse proxy (nginx, Traefik) with SSL certificates
   - Configure TLS/SSL for database connections

4. **✅ Configure CORS:**
   ```bash
   export CORS_ORIGINS=https://yourdomain.com,https://admin.yourdomain.com
   ```

5. **✅ Database security:**
   - Use strong passwords
   - Enable SSL/TLS connections
   - Restrict network access (firewall rules)
   - Set up regular backups
   - Use connection pooling

6. **✅ Docker Secrets (recommended):**
   ```bash
   # Create Docker secrets
   echo "your-jwt-secret" | docker secret create jwt_secret -
   echo "your-db-password" | docker secret create database_password -
   
   # Use in docker-compose.prod.yml
   docker stack deploy -c docker-compose.prod.yml flagship
   ```

7. **✅ Monitoring:**
   - Set up log aggregation
   - Monitor database connections
   - Track API usage and errors
   - Set up alerts for security events

### Using Docker Compose for Production

```bash
# 1. Copy and configure .env file
cp .env.example .env
# Edit .env with production values

# 2. Use production compose file
docker-compose -f docker-compose.prod.yml up -d

# 3. Verify deployment
docker-compose -f docker-compose.prod.yml ps
docker-compose -f docker-compose.prod.yml logs -f server
```

## Docker Build

```bash
docker build -t flagship-server -f flagship-server/Dockerfile .
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/flagship \
  -e JWT_SECRET=your-secret \
  flagship-server
```
