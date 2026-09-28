# Multi-stage Docker build for DropWatch Spring Boot Backend (relative to backend/)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY dropwatch-parent/pom.xml dropwatch-parent/
COPY dropwatch-core/pom.xml dropwatch-core/
COPY dropwatch-flags/pom.xml dropwatch-flags/
COPY dropwatch-scraper/pom.xml dropwatch-scraper/
COPY dropwatch-messaging/pom.xml dropwatch-messaging/
COPY dropwatch-notify/pom.xml dropwatch-notify/
COPY dropwatch-api/pom.xml dropwatch-api/
COPY dropwatch-app/pom.xml dropwatch-app/

RUN ./mvnw dependency:go-offline -B -pl dropwatch-app -am || true

COPY . .
RUN ./mvnw clean package -DskipTests -pl dropwatch-app -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S dropwatch && adduser -S dropwatch -G dropwatch
USER dropwatch

COPY --from=builder /app/dropwatch-app/target/dropwatch-app-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java", "-jar", "app.jar"]
