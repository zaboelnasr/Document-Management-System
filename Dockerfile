# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy POM and source
COPY pom.xml .
COPY src ./src

# --- NEW SECTION: install local indexing-worker dependency ---
# Copy the built indexing-worker JAR from your host into the container
COPY ../indexing-worker/target/indexing-worker-1.0.0.jar /tmp/indexing-worker.jar

# Install it into Maven local repo inside container
RUN mvn install:install-file \
    -Dfile=/tmp/indexing-worker.jar \
    -DgroupId=com.dms \
    -DartifactId=indexing-worker \
    -Dversion=1.0.0 \
    -Dpackaging=jar

# Now build backend
RUN mvn -q -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

# curl for docker healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
