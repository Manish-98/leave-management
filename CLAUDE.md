# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot leave management application built with Java 17 and Gradle. The project uses PostgreSQL as the database and includes comprehensive Spring ecosystem integrations.

## Build and Development Commands

### Quick Start (Recommended)
- `make quick-start` - Build and run everything with Docker in one command
- `make help` - Show all available commands

### Local Development
- `make build` - Build the Spring Boot application (same as `./gradlew build`)
- `make run` - Run the application locally (requires local PostgreSQL)
- `make test` - Run all tests (uses H2 in-memory database for Docker/CI compatibility)
- `make clean` - Clean build artifacts

### Docker Development
- `make docker-build` - Build Docker image (includes automated tests)
- `make docker-run` - Start all services with Docker Compose
- `make docker-stop` - Stop all Docker services
- `make docker-clean` - Remove Docker containers, images, and volumes
- `make docker-logs` - Show logs from all services
- `make docker-ps` - Show running containers
- `make docker-admin` - Start services with pgAdmin included
- `make health` - Check application health status

### Manual Gradle Commands
- `./gradlew build` - Build the project
- `./gradlew bootRun` - Run the Spring Boot application
- `./gradlew test` - Run all tests
- `./gradlew clean` - Clean build artifacts

### Database
- The application uses Flyway for database migrations
- Docker setup includes PostgreSQL container automatically
- For local development, PostgreSQL must be installed and running on localhost:5432
- Database name: `leave_management`, user: `postgres`, password: `postgres`

## Technology Stack

- **Framework**: Spring Boot 4.0.0
- **Language**: Java 17
- **Build Tool**: Gradle with dependency management
- **Database**: PostgreSQL with JPA/Hibernate
- **Migration**: Flyway
- **Security**: Spring Security
- **Validation**: Spring Boot Validation
- **Testing**: JUnit 5 with Spring Boot Test, H2 in-memory database

## Testing Configuration

### Test Database
- **H2 In-Memory Database**: Fast, lightweight in-memory database used for all testing scenarios
- Provides consistent test performance across local development, Docker builds, and CI/CD pipelines
- Uses H2 dialect for optimal compatibility

### Test Configuration Files
- `src/test/resources/application-test.properties` - Test profile configuration
- Tests use the "test" profile automatically via `@ActiveProfiles("test")`

### Test Execution
- Tests run automatically during Docker builds
- Local development: `make test` or `./gradlew test`
- Docker builds include tests by default (no test skipping)

## Project Structure

```
src/
├── main/
│   ├── java/one/june/leave_management/
│   │   └── LeaveManagementApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/one/june/leave_management/
        └── LeaveManagementApplicationTests.java
```

## Package Convention

The project uses the package name `one.june.leave_management` (with underscores) instead of the invalid `one.june.leave-management` format.

## Key Dependencies and Integrations

- **Spring WebMvc**: For REST API development
- **Spring Data JPA**: For database operations
- **Spring Security**: For authentication and authorization
- **Spring Boot Actuator**: For application monitoring and management
- **Validation**: For request/response validation
- **Flyway**: For database version control

## Development Notes

- The project is currently in early development stage with minimal implemented features
- Main application class is `LeaveManagementApplication.java`
- Default test configuration available in `LeaveManagementApplicationTests.java`
- Application name is configured as "leave-management" in `application.properties`

## Configuration

- **Main config**: `src/main/resources/application.properties`
- **Docker config**: `src/main/resources/application-docker.properties`
- **Database**: PostgreSQL with connection details configured in properties files
- **Flyway migrations**: Place in `src/main/resources/db/migration/` when implemented

## Docker Setup

The project includes complete Docker configuration:

- **Dockerfile**: Multi-stage build for optimized production image
- **docker-compose.yml**: Includes PostgreSQL database and optional pgAdmin
- **Services**:
  - `app`: Spring Boot application (port 8080)
  - `postgres`: PostgreSQL 15 (port 5432)
  - `pgadmin`: Optional admin tool (port 5050, use `make docker-admin`)

### Docker Ports
- Application: http://localhost:8080
- PostgreSQL: localhost:5432
- pgAdmin: http://localhost:5050 (when enabled)
- Health endpoint: http://localhost:8080/actuator/health

### Environment Variables for Docker
- `SPRING_PROFILES_ACTIVE=docker` - Uses Docker-specific configuration
- `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/leave_management`
- `SPRING_DATASOURCE_USERNAME=postgres`
- `SPRING_DATASOURCE_PASSWORD=postgres`