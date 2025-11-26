#!/bin/bash
# Database Migration Script
# Usage: ./scripts/migrate.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "🔄 Running database migrations..."

# Check if .env file exists
if [ ! -f "$PROJECT_DIR/.env" ]; then
    echo "❌ Error: .env file not found. Please create it from .env.example"
    exit 1
fi

# Load environment variables
set -a
source "$PROJECT_DIR/.env"
set +a

# Check if database is running
if ! docker-compose -f docker-compose.prod.yml ps postgres | grep -q "Up"; then
    echo "📦 Starting database..."
    docker-compose -f docker-compose.prod.yml up -d postgres
    sleep 5
fi

# Run migrations (migrations are run automatically on server start)
# This script can be extended to run manual migrations if needed
echo "✅ Migrations will be run automatically on server start"
echo "💡 To run migrations manually, start the server: docker-compose -f docker-compose.prod.yml up server"

