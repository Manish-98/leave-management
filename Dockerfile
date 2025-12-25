# Use the official Gradle image to build the application
FROM gradle:8-jdk17 AS build

# Set working directory
WORKDIR /app

# Copy gradle wrapper files first to leverage Docker layer caching
COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .

# Copy source code
COPY src src

# Build the application (skipping tests for now)
RUN ./gradlew build -x test --no-daemon

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-alpine

# Install necessary packages
RUN apk add --no-cache curl

# Set working directory
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/build/libs/ ./

# Expose the port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "leave-management-0.0.1-SNAPSHOT.jar"]