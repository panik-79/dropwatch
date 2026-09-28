# DropWatch Makefile

.PHONY: up down dev build test seed

up:
	docker compose -f infra/docker-compose.yml up -d

down:
	docker compose -f infra/docker-compose.yml down -v

dev-backend:
	cd backend && mvn spring-boot:run -pl dropwatch-app

dev-frontend:
	cd frontend && npm start

build:
	cd backend && mvn clean package -DskipTests

test:
	cd backend && mvn test

seed:
	powershell -File scripts/seed-data.ps1
