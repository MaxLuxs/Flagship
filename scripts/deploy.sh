#!/bin/bash
# Flagship Server Deployment Script
# Usage: ./scripts/deploy.sh [environment]

set -e

ENVIRONMENT=${1:-production}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "🚀 Deploying Flagship Server to $ENVIRONMENT..."

# Check if .env file exists
if [ ! -f "$PROJECT_DIR/.env" ]; then
    echo "❌ Error: .env file not found. Please create it from .env.example"
    exit 1
fi

# Load environment variables
set -a
source "$PROJECT_DIR/.env"
set +a

# Run database migrations
echo "📦 Running database migrations..."
cd "$PROJECT_DIR"
docker-compose -f docker-compose.prod.yml exec -T postgres psql -U "$DATABASE_USER" -d "$POSTGRES_DB" -c "SELECT 1" || {
    echo "❌ Database not available. Starting services..."
    docker-compose -f docker-compose.prod.yml up -d postgres
    sleep 5
}

# Build and start services
echo "🔨 Building and starting services..."
docker-compose -f docker-compose.prod.yml up -d --build

# Wait for health check
echo "⏳ Waiting for services to be healthy..."
timeout=60
elapsed=0
while [ $elapsed -lt $timeout ]; do
    if curl -f http://localhost:${SERVER_PORT:-8080}/health/liveness > /dev/null 2>&1; then
        echo "✅ Services are healthy!"
        break
    fi
    sleep 2
    elapsed=$((elapsed + 2))
done

if [ $elapsed -ge $timeout ]; then
    echo "❌ Services failed to become healthy"
    docker-compose -f docker-compose.prod.yml logs
    exit 1
fi

echo "✅ Deployment completed successfully!"
echo "🌐 Server is available at http://localhost:${SERVER_PORT:-8080}"

