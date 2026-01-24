package com.dms.schedule;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XmlBatchDockerIntegrationTest {

    @Test
    void processXml_viaDockerCompose() throws Exception {
        if (!isScheduleWorkerReady()) {
            runDockerComposeUp();
            waitForScheduleWorker();
        }

        try {
            triggerBatch();
        } finally {
            stopDockerCompose();
        }
    }

    private static BatchResponse triggerBatch() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8091/api/batch/run"))
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return parseBatchResponse(response.body());
    }

    private static void waitForScheduleWorker() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        URI uri = URI.create("http://localhost:8091/api/batch/run");
        long deadline = System.currentTimeMillis() + Duration.ofMinutes(2).toMillis();

        while (System.currentTimeMillis() < deadline) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(Duration.ofSeconds(5))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return;
                }
            } catch (Exception ignored) {
            }
            Thread.sleep(2000);
        }

        throw new IllegalStateException("Schedule worker did not become ready in time.");
    }

    private static boolean isScheduleWorkerReady() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8091/api/batch/run"))
                    .timeout(Duration.ofSeconds(2))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private static void runDockerComposeUp() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("docker", "compose", "up", "-d", "--build");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (var in = process.getInputStream()) {
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
        try (var in = process.getInputStream()) {
            in.transferTo(output);
        }

        int exit = process.waitFor();
        if (exit != 0) {
            String text = output.toString(StandardCharsets.UTF_8);
            throw new IllegalStateException("docker compose stop failed: " + text);
        }
    }

    private static BatchResponse parseBatchResponse(String body) {
        int files = extractInt(body, "processedFiles");
        int docs = extractInt(body, "processedDocuments");
        return new BatchResponse(files, docs);
    }

    private static int extractInt(String body, String field) {
        String key = "\"" + field + "\":";
        int idx = body.indexOf(key);
        if (idx < 0) {
            return 0;
        }
        int start = idx + key.length();
        int end = start;
        while (end < body.length() && Character.isDigit(body.charAt(end))) {
            end++;
        }
        try {
            return Integer.parseInt(body.substring(start, end));
        } catch (Exception e) {
            return 0;
        }
    }

    private static class BatchResponse {
        private final int processedFiles;
        private final int processedDocuments;

        private BatchResponse(int processedFiles, int processedDocuments) {
            this.processedFiles = processedFiles;
            this.processedDocuments = processedDocuments;
        }
    }

}
