#!/bin/bash
set -e

BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/numina_$TIMESTAMP.sql"

mkdir -p $BACKUP_DIR

echo "📦 Creating database backup..."
docker-compose exec -T postgres pg_dump -U numina numina > $BACKUP_FILE

echo "✅ Backup created: $BACKUP_FILE"

# Optionally compress the backup
gzip $BACKUP_FILE || echo "⚠️  Compression skipped (gzip not available)"
echo "✅ Backup compressed: $BACKUP_FILE.gz"
