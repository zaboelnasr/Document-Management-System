# indexing-worker/Dockerfile
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/indexing-worker-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
