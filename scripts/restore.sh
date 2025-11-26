#!/bin/bash
# Database Restore Script
# Usage: ./scripts/restore.sh <backup_file>

set -e

if [ -z "$1" ]; then
    echo "❌ Error: Backup file not specified"
    echo "Usage: ./scripts/restore.sh <backup_file>"
    exit 1
fi

BACKUP_FILE="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ Error: Backup file not found: $BACKUP_FILE"
    exit 1
fi

echo "⚠️  WARNING: This will restore the database from backup!"
echo "⚠️  All current data will be lost!"
read -p "Are you sure you want to continue? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "❌ Restore cancelled"
    exit 1
fi

# Check if .env file exists
if [ ! -f "$PROJECT_DIR/.env" ]; then
    echo "❌ Error: .env file not found. Please create it from .env.example"
    exit 1
fi

# Load environment variables
set -a
source "$PROJECT_DIR/.env"
set +a

echo "🔄 Restoring database from: $BACKUP_FILE"

# Check if backup is compressed
if [[ "$BACKUP_FILE" == *.gz ]]; then
    echo "📦 Decompressing backup..."
    gunzip -c "$BACKUP_FILE" | docker-compose -f docker-compose.prod.yml exec -T postgres psql -U "$DATABASE_USER" "$POSTGRES_DB"
else
    docker-compose -f docker-compose.prod.yml exec -T postgres psql -U "$DATABASE_USER" "$POSTGRES_DB" < "$BACKUP_FILE"
fi

echo "✅ Database restored successfully!"

