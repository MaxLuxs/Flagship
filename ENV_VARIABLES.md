# Environment Variables Documentation

This document describes all environment variables used by Flagship Server.

## Database Configuration

### `DATABASE_URL`
- **Description**: PostgreSQL connection URL
- **Default**: `jdbc:postgresql://localhost:5432/flagship`
- **Example**: `jdbc:postgresql://db.example.com:5432/flagship`
- **Required**: Yes

### `DATABASE_USER`
- **Description**: PostgreSQL username
- **Default**: `flagship`
- **Required**: Yes

### `DATABASE_PASSWORD`
- **Description**: PostgreSQL password
- **Default**: `flagship_dev_password` (development only)
- **Required**: Yes
- **Security**: Use strong password in production

### `DB_POOL_MAX_SIZE`
- **Description**: Maximum number of connections in the pool
- **Default**: `10`
- **Type**: Integer

### `DB_POOL_MIN_IDLE`
- **Description**: Minimum number of idle connections
- **Default**: `2`
- **Type**: Integer

### `DB_CONNECTION_TIMEOUT`
- **Description**: Connection timeout in milliseconds
- **Default**: `30000` (30 seconds)
- **Type**: Integer

### `DB_IDLE_TIMEOUT`
- **Description**: Idle connection timeout in milliseconds
- **Default**: `600000` (10 minutes)
- **Type**: Integer

### `DB_MAX_LIFETIME`
- **Description**: Maximum connection lifetime in milliseconds
- **Default**: `1800000` (30 minutes)
- **Type**: Integer

### `DB_LEAK_DETECTION_THRESHOLD`
- **Description**: Connection leak detection threshold in milliseconds
- **Default**: `60000` (1 minute)
- **Type**: Integer

## Database SSL (Production)

### `DB_SSL`
- **Description**: Enable SSL for database connections
- **Default**: `false`
- **Type**: Boolean
- **Production**: Should be `true`

### `DB_SSLMODE`
- **Description**: SSL mode (require, verify-ca, verify-full)
- **Default**: `require`
- **Options**: `disable`, `allow`, `prefer`, `require`, `verify-ca`, `verify-full`

### `DB_SSL_CERT`
- **Description**: Path to SSL client certificate
- **Default**: Not set
- **Type**: String (file path)

### `DB_SSL_KEY`
- **Description**: Path to SSL client key
- **Default**: Not set
- **Type**: String (file path)

### `DB_SSL_ROOT_CERT`
- **Description**: Path to SSL root certificate
- **Default**: Not set
- **Type**: String (file path)

## Server Configuration

### `SERVER_PORT`
- **Description**: Server port
- **Default**: `8080`
- **Type**: Integer

### `SERVER_HOST`
- **Description**: Server host
- **Default**: `0.0.0.0`
- **Type**: String

## Authentication

### `JWT_SECRET`
- **Description**: Secret key for JWT token signing
- **Default**: `change-me-in-production-use-strong-secret-key`
- **Required**: Yes
- **Security**: Must be at least 32 characters in production
- **Warning**: Never commit this to version control

## CORS Configuration

### `CORS_ORIGINS`
- **Description**: Comma-separated list of allowed CORS origins
- **Default**: Not set (allows all origins)
- **Example**: `http://localhost:3000,https://app.example.com`
- **Production**: Should be restricted to specific domains

## Logging

### `LOG_LEVEL`
- **Description**: Logging level
- **Default**: `INFO`
- **Options**: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`
- **Production**: Should be `INFO` or `WARN`

### `LOG_FORMAT`
- **Description**: Log format (text or json)
- **Default**: `text`
- **Options**: `text`, `json`
- **Production**: `json` for structured logging

## Environment

### `NODE_ENV`
- **Description**: Environment name
- **Default**: `development`
- **Options**: `development`, `staging`, `production`

## Production Checklist

Before deploying to production, ensure:

- [ ] `DATABASE_PASSWORD` is strong and secure
- [ ] `JWT_SECRET` is at least 32 characters and unique
- [ ] `DB_SSL` is set to `true`
- [ ] `CORS_ORIGINS` is restricted to specific domains
- [ ] `LOG_LEVEL` is set to `INFO` or `WARN`
- [ ] All sensitive variables are stored in secrets management
- [ ] Database connection pool is tuned for your workload
- [ ] SSL certificates are properly configured

## Example .env File

```bash
# Database
DATABASE_URL=jdbc:postgresql://db.example.com:5432/flagship
DATABASE_USER=flagship
DATABASE_PASSWORD=your_secure_password_here
DB_SSL=true
DB_SSLMODE=require

# Server
SERVER_PORT=8080
SERVER_HOST=0.0.0.0

# Security
JWT_SECRET=your-very-long-and-secure-secret-key-minimum-32-characters
CORS_ORIGINS=https://app.example.com,https://admin.example.com

# Logging
LOG_LEVEL=INFO
LOG_FORMAT=json

# Environment
NODE_ENV=production
```

