package com.dms.documentmanagementsystem.controller;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentUploadIntegrationTest {

    @Test
    void uploadPdf_viaDockerCompose() throws Exception {
        if (!isBackendReady()) {
            runDockerComposeUp();
            waitForBackendReadiness();
        }

        try {
            byte[] pdfBytes = loadResource("fixtures/Integration_test_worked.pdf");
            String boundary = "----DMSBoundary" + UUID.randomUUID();
            byte[] body = buildMultipartBody(boundary, pdfBytes);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/documents/upload"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("Integration_test_worked.pdf"));
        } finally {
            stopDockerCompose();
        }
    }

    private static void runDockerComposeUp() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("docker", "compose", "up", "-d", "--build");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream in = process.getInputStream()) {
            in.transferTo(output);
        }

        int exit = process.waitFor();
        String text = output.toString(StandardCharsets.UTF_8);
        if (exit != 0 && !text.contains("already exists")) {
            throw new IllegalStateException("docker compose failed: " + text);
        }
    }

    private static void stopDockerCompose() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("docker", "compose", "stop");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream in = process.getInputStream()) {
            in.transferTo(output);
        }

        int exit = process.waitFor();
        if (exit != 0) {
            String text = output.toString(StandardCharsets.UTF_8);
            throw new IllegalStateException("docker compose stop failed: " + text);
        }
    }

    private static void waitForBackendReadiness() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        URI uri = URI.create("http://localhost:8080/actuator/health/readiness");
        long deadline = System.currentTimeMillis() + Duration.ofMinutes(2).toMillis();

        while (System.currentTimeMillis() < deadline) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200 && response.body().contains("\"status\":\"UP\"")) {
                    return;
                }
            } catch (IOException ignored) {
                // backend not ready yet
            }

            Thread.sleep(2000);
        }

        throw new IllegalStateException("Backend did not become ready in time.");
    }

    private static boolean isBackendReady() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/actuator/health/readiness"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().contains("\"status\":\"UP\"");
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] loadResource(String path) throws IOException {
        try (InputStream in = DocumentUploadIntegrationTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "Missing test resource: " + path);
            return in.readAllBytes();
        }
    }

    private static byte[] buildMultipartBody(String boundary, byte[] fileBytes) throws IOException {
        String fileHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"Integration_test_worked.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n";
        String summaryPart = "\r\n--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"summary\"\r\n\r\n"
                + "integration test\r\n";
        String end = "--" + boundary + "--\r\n";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(fileHeader.getBytes(StandardCharsets.UTF_8));
        out.write(fileBytes);
        out.write(summaryPart.getBytes(StandardCharsets.UTF_8));
        out.write(end.getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}
