.PHONY: help build run stop clean logs test docker-build docker-run docker-stop docker-clean docker-logs docker-ps

# Default target
help:
	@echo "Available commands:"
	@echo "  build          - Build the Spring Boot application"
	@echo "  run            - Run the application locally (requires PostgreSQL)"
	@echo "  stop           - Stop the locally running application"
	@echo "  test           - Run tests (uses H2 in-memory database)"
	@echo "  clean          - Clean build artifacts"
	@echo ""
	@echo "Docker commands:"
	@echo "  docker-build   - Build Docker image (includes tests)"
	@echo "  docker-run     - Start all services with Docker Compose"
	@echo "  docker-stop    - Stop all Docker services"
	@echo "  docker-clean   - Remove Docker containers, images, and volumes"
	@echo "  docker-logs    - Show logs from all services"
	@echo "  docker-ps      - Show running containers"
	@echo "  docker-admin   - Start services with pgAdmin included"
	@echo ""
	@echo "Development commands:"
	@echo "  dev-setup      - Setup development environment"
	@echo "  dev-restart    - Restart Docker services"

# Local development commands
build:
	./gradlew build

run:
	./gradlew bootRun

stop:
	pkill -f "LeaveManagementApplication" || true

test:
	./gradlew test

clean:
	./gradlew clean

# Docker commands
docker-build:
	docker build -t leave-management:latest .

docker-run:
	docker-compose up -d

docker-stop:
	docker-compose down

docker-clean: docker-stop
	docker system prune -f
	docker volume prune -f

docker-logs:
	docker-compose logs -f

docker-ps:
	docker-compose ps

docker-admin:
	docker-compose --profile admin up -d

# Development commands
dev-setup:
	@echo "Setting up development environment..."
	@echo "1. Make sure PostgreSQL is installed and running on localhost:5432"
	@echo "2. Create database 'leave_management'"
	@echo "3. Run 'make run' to start the application"
	@echo ""
	@echo "Or use Docker: 'make docker-run'"

dev-restart: docker-stop docker-run

# Utility commands
health:
	curl -f http://localhost:8080/actuator/health || echo "Application not running"

db-connect:
	psql -h localhost -p 5432 -U postgres -d leave_management

# Quick start commands
quick-start: docker-build docker-run
	@echo "Application is starting..."
	@echo "Wait for the services to be ready..."
	@echo "Check health with: make health"
	@echo "View logs with: make docker-logs"

quick-stop: docker-stop