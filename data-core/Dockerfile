FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy project files
COPY pom.xml .
COPY data-core/pom.xml ./data-core/
COPY data-core/src ./data-core/src

# Build with Maven
RUN apk add --no-cache maven && \
    cd data-core && \
    mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/data-core/target/*.jar app.jar

# Add health check dependencies
RUN apk add --no-cache wget

# Expose port
EXPOSE 9090

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -q --spider http://localhost:9090/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
