# Multi-stage Docker build for DropWatch Spring Boot Backend (Backend Context)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy backend files
COPY . .

RUN chmod +x ./mvnw
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
