FROM gradle:8.5-jdk21 AS build

WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts gradle/ ./
COPY flagship-server/build.gradle.kts flagship-server/
COPY flagship-core/build.gradle.kts flagship-core/
COPY flagship-provider-rest/build.gradle.kts flagship-provider-rest/

# Copy source code
COPY flagship-server/src flagship-server/src
COPY flagship-core/src flagship-core/src
COPY flagship-provider-rest/src flagship-provider-rest/src

# Build
RUN gradle :flagship-server:build --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy built JAR
COPY --from=build /app/flagship-server/build/libs/*.jar app.jar

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

