# =============================================================================
# Backend Gest_POV — image monapp-backend:1.0.0
# Build multi-étapes : compilation Maven puis JAR exécutable Spring Boot
# =============================================================================

# --- Étape 1 : compilation ---
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Cache des dépendances Maven
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# --- Étape 2 : runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# wget pour HEALTHCHECK
RUN apk add --no-cache wget

# Répertoires persistés via volumes Docker
RUN mkdir -p /app/uploads /app/gest-pov-data

COPY --from=build /app/target/gest-pov-backend.jar /app/app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
