# -------- Build stage --------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy sources and build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# -------- Runtime stage (Ubuntu/Debian base with OCR tools) --------
FROM eclipse-temurin:21-jdk-jammy

# Install Ghostscript + Tesseract (incl. German language pack)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
      tesseract-ocr \
      tesseract-ocr-deu \
      ghostscript \
      curl \
      ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Optional but nice for predictable output
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8 TZ=UTC

WORKDIR /app

# Copy the built jar from the build stage
# (matches any jar produced by your Maven build)
COPY --from=build /app/target/*.jar /app/app.jar

# Run the worker
ENTRYPOINT ["java","-jar","/app/app.jar"]
