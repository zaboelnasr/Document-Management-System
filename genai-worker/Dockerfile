# --- Build Stage ---
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Kopiere das gesamte genai-worker Projekt in das Image
COPY . .

# Build ohne Tests
RUN mvn clean package -DskipTests

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Jar aus dem Build-Container holen
COPY --from=build /app/target/*.jar app.jar

# WICHTIG: Der API-Key wird später über docker-compose gesetzt
ENV GEMINI_API_KEY=""

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]