# Multi-stage Docker build for DropWatch Spring Boot Backend
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy context files
COPY . .

# Build executable jar under any build context (root or backend)
RUN if [ -d "backend" ]; then \
      cd backend && sh mvnw clean package -DskipTests -pl dropwatch-app -am && cp dropwatch-app/target/dropwatch-app-1.0.0-SNAPSHOT.jar /app/app.jar; \
    else \
      sh mvnw clean package -DskipTests -pl dropwatch-app -am && cp dropwatch-app/target/dropwatch-app-1.0.0-SNAPSHOT.jar /app/app.jar; \
    fi

# Production runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S dropwatch && adduser -S dropwatch -G dropwatch
USER dropwatch

COPY --from=builder /app/app.jar app.jar

EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java", "-jar", "app.jar"]
