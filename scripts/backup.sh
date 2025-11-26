#!/bin/bash
# Database Backup Script
# Usage: ./scripts/backup.sh [backup_name]

set -e

BACKUP_NAME=${1:-backup_$(date +%Y%m%d_%H%M%S)}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKUP_DIR="$PROJECT_DIR/backups"

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

echo "💾 Creating database backup: $BACKUP_NAME"

# Check if .env file exists
if [ ! -f "$PROJECT_DIR/.env" ]; then
    echo "❌ Error: .env file not found. Please create it from .env.example"
    exit 1
fi

# Load environment variables
set -a
source "$PROJECT_DIR/.env"
set +a

# Create backup
BACKUP_FILE="$BACKUP_DIR/$BACKUP_NAME.sql"
docker-compose -f docker-compose.prod.yml exec -T postgres pg_dump -U "$DATABASE_USER" "$POSTGRES_DB" > "$BACKUP_FILE"

# Compress backup
gzip "$BACKUP_FILE"
BACKUP_FILE="${BACKUP_FILE}.gz"

echo "✅ Backup created: $BACKUP_FILE"
echo "📊 Backup size: $(du -h "$BACKUP_FILE" | cut -f1)"

# Keep only last 10 backups
cd "$BACKUP_DIR"
ls -t *.sql.gz | tail -n +11 | xargs -r rm

echo "✅ Backup completed!"

