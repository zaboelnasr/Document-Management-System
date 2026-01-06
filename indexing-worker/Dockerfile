# indexing-worker/Dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/indexing-worker-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
