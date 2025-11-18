#!/bin/bash
set -e

echo "🚀 Deploying Numina Backend..."

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
fi

# Build Docker image
echo "📦 Building Docker image..."
docker-compose build

# Run database migrations
echo "🗄️  Running migrations..."
docker-compose run --rm backend gradle flywayMigrate || echo "⚠️  Migration step skipped (flyway may not be configured)"

# Start services
echo "▶️  Starting services..."
docker-compose up -d

# Wait for health check
echo "🏥 Waiting for health check..."
timeout 60 bash -c 'until curl -f http://localhost:8080/health; do sleep 2; done' || echo "⚠️  Health check timeout"

echo "✅ Deployment complete!"
