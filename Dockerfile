FROM gradle:8.5-jdk17 AS build

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src

RUN gradle buildFatJar --no-daemon

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/build/libs/*-all.jar app.jar

EXPOSE 8080

ENV DATABASE_URL=""
ENV DB_USER=""
ENV DB_PASSWORD=""
ENV JWT_SECRET=""
ENV JWT_ISSUER="numina-backend"
ENV JWT_AUDIENCE="numina-clients"
ENV JWT_REALM="numina"
ENV REDIS_HOST="localhost"
ENV REDIS_PORT="6379"
ENV FCM_CREDENTIALS_PATH=""
ENV ENVIRONMENT="production"

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
