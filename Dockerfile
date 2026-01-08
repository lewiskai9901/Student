# Multi-stage build for Student Management System

# ============ Stage 1: Build Backend ============
FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /app/backend

# Copy pom.xml first to cache dependencies
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY backend/src ./src
RUN mvn package -DskipTests -B

# ============ Stage 2: Build Frontend ============
FROM node:18-alpine AS frontend-builder

WORKDIR /app/frontend

# Copy package files first to cache dependencies
COPY frontend/package*.json ./
RUN npm ci

# Copy source and build
COPY frontend/ ./
RUN npm run build

# ============ Stage 3: Production Image ============
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Student Management System Team"
LABEL version="1.0.0"
LABEL description="Student Management System - Production Image"

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Copy backend JAR
COPY --from=backend-builder /app/backend/target/*.jar app.jar

# Copy frontend build to static resources
COPY --from=frontend-builder /app/frontend/dist ./static

# Create necessary directories
RUN mkdir -p /app/logs /app/uploads /app/config && \
    chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

# JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom"

# Spring profiles
ENV SPRING_PROFILES_ACTIVE=prod

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
