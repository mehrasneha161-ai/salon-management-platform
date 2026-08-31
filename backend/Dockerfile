# syntax=docker/dockerfile:1

# ---- Build stage: compile & package the Spring Boot jar ----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Resolve dependencies first so this layer is cached until pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copy sources and build (tests are run in CI, skipped for the image build).
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage: slim JRE, non-root ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as an unprivileged user.
RUN addgroup -S salon && adduser -S salon -G salon

COPY --from=builder /app/target/*.jar app.jar
RUN chown salon:salon app.jar
USER salon

# Container-aware heap sizing; profile is driven by SPRING_PROFILES_ACTIVE (env).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
