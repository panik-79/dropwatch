# Multi-stage Docker build for DropWatch Spring Boot Backend
# Build stage: Use JDK with Maven pre-installed (avoids mvnw issues on Linux)
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy source from backend directory context
COPY . .

# Build the application jar
RUN mvn clean package -DskipTests -pl dropwatch-app -am -q

# Production runtime stage: minimal JRE image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S dropwatch && adduser -S dropwatch -G dropwatch
USER dropwatch

COPY --from=builder /app/dropwatch-app/target/dropwatch-app-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080
ENV PORT=8080

ENTRYPOINT ["java", "-jar", "app.jar"]
