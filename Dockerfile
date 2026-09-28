# Multi-stage Docker build for DropWatch Spring Boot Backend
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper & POM files first for caching
COPY backend/mvnw .
COPY backend/.mvn .mvn
COPY backend/pom.xml .
COPY backend/dropwatch-parent/pom.xml dropwatch-parent/
COPY backend/dropwatch-core/pom.xml dropwatch-core/
COPY backend/dropwatch-flags/pom.xml dropwatch-flags/
COPY backend/dropwatch-scraper/pom.xml dropwatch-scraper/
COPY backend/dropwatch-messaging/pom.xml dropwatch-messaging/
COPY backend/dropwatch-notify/pom.xml dropwatch-notify/
COPY backend/dropwatch-api/pom.xml dropwatch-api/
COPY backend/dropwatch-app/pom.xml dropwatch-app/

RUN ./mvnw dependency:go-offline -B -pl dropwatch-app -am || true

# Copy source code and build executable jar
COPY backend/ .
RUN ./mvnw clean package -DskipTests -pl dropwatch-app -am

# Production runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S dropwatch && adduser -S dropwatch -G dropwatch
USER dropwatch

COPY --from=builder /app/dropwatch-app/target/dropwatch-app-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java", "-jar", "app.jar"]
