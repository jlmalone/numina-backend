# Numina Backend Deployment Guide

## Prerequisites
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+
- Firebase project with FCM enabled

## Quick Start

1. **Clone and configure**:
   ```bash
   git clone <repo>
   cd numina-backend
   cp .env.example .env
   # Edit .env with your values
   ```

2. **Deploy with Docker**:
   ```bash
   ./scripts/deploy.sh
   ```

3. **Verify deployment**:
   ```bash
   curl http://localhost:8080/health
   ```

## Manual Deployment

1. **Build**:
   ```bash
   ./gradlew buildFatJar
   ```

2. **Run migrations**:
   ```bash
   ./gradlew flywayMigrate
   ```

3. **Start server**:
   ```bash
   java -jar build/libs/numina-backend-all.jar
   ```

## Production Checklist
- [ ] Set strong JWT_SECRET
- [ ] Configure CORS allowed hosts
- [ ] Set up SSL/TLS certificates
- [ ] Configure firewall rules
- [ ] Set up database backups
- [ ] Configure log aggregation
- [ ] Set up monitoring/alerts
- [ ] Review rate limiting settings

## Monitoring
- Health: `GET /health`
- Readiness: `GET /ready`
- Metrics: `GET /metrics` (Prometheus format)

## Backup
```bash
./scripts/backup-db.sh
```

## Environment Variables

### Required
- `DATABASE_URL`: PostgreSQL connection URL
- `DB_USER`: Database username
- `DB_PASSWORD`: Database password
- `JWT_SECRET`: Secret key for JWT signing

### Optional
- `JWT_ISSUER`: JWT issuer (default: numina-backend)
- `JWT_AUDIENCE`: JWT audience (default: numina-clients)
- `JWT_REALM`: JWT realm (default: numina)
- `REDIS_HOST`: Redis host (default: localhost)
- `REDIS_PORT`: Redis port (default: 6379)
- `FCM_CREDENTIALS_PATH`: Path to Firebase credentials JSON
- `ALLOWED_HOSTS`: Comma-separated list of allowed CORS hosts
- `ENVIRONMENT`: Environment name (development/production)

## Docker Commands

### Start services
```bash
docker-compose up -d
```

### Stop services
```bash
docker-compose down
```

### View logs
```bash
docker-compose logs -f backend
```

### Restart backend
```bash
docker-compose restart backend
```

### Rebuild and restart
```bash
docker-compose up -d --build
```

## Testing

### Run all tests
```bash
./gradlew test
```

### Run E2E tests only
```bash
./gradlew test --tests "com.numina.e2e.*"
```

### Run specific test class
```bash
./gradlew test --tests "com.numina.e2e.AuthFlowTest"
```

## Troubleshooting

### Health check failing
- Check if database is accessible
- Check if Redis is accessible
- Review application logs: `docker-compose logs backend`

### Database connection issues
- Verify DATABASE_URL is correct
- Check if PostgreSQL container is running: `docker-compose ps`
- Check PostgreSQL logs: `docker-compose logs postgres`

### Redis connection issues
- Check if Redis container is running: `docker-compose ps`
- Check Redis logs: `docker-compose logs redis`

## Production Deployment Tips

1. **Use environment-specific configs**: Set `ENVIRONMENT=production` and ensure production config is loaded
2. **Enable HTTPS**: Use a reverse proxy (nginx, Traefik) with SSL/TLS certificates
3. **Database migrations**: Always backup before running migrations
4. **Monitoring**: Set up alerting for health check failures
5. **Logging**: Configure centralized logging (e.g., ELK stack, CloudWatch)
6. **Secrets management**: Use a secrets manager (Vault, AWS Secrets Manager) instead of .env files
7. **Container orchestration**: Consider Kubernetes for production deployments

## Support

For issues and questions, please refer to the project repository or contact the development team.
