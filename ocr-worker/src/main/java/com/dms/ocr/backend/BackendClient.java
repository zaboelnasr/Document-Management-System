package com.dms.ocr.backend;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackendClient {

    private final RestTemplate restTemplate;

    @Value("${backend.base-url}")
    private String backendBaseUrl;

    public void updateSummary(Long documentId, String summary) {
        String url = backendBaseUrl + "/api/documents/" + documentId + "/summary";
        try {
            log.info("Updating document summary in backend: documentId={}, url={}", documentId, url);

            // ---------- WICHTIGER TEIL ----------
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = Map.of("summary", summary);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);
            // -----------------------------------

            log.info("Backend summary update successful for documentId={}, status={}",
                    documentId, response.getStatusCode());
        } catch (Exception e) {
            log.error("Error updating summary in backend for documentId={}", documentId, e);
            throw new IllegalStateException("Failed to update backend summary", e);
        }
    }

    public void updateOcrStatus(Long documentId, String status) {
        String url = backendBaseUrl + "/api/documents/" + documentId + "/ocr-status";
        try {
            log.info("Updating OCR status in backend: documentId={}, status={}, url={}",
                    documentId, status, url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = Map.of("status", status);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Void> response =
                    restTemplate.exchange(url, HttpMethod.PATCH, request, Void.class);

            log.info("Backend OCR status update successful for documentId={}, status={}",
                    documentId, response.getStatusCode());
        } catch (Exception e) {
            log.error("Error updating OCR status in backend for documentId={}", documentId, e);
            throw new IllegalStateException("Failed to update backend OCR status", e);
        }
    }
}
