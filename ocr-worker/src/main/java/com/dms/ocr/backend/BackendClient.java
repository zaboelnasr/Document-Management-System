package com.dms.ocr.backend;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackendClient {

    private final RestTemplate restTemplate;

    @Value("${BACKEND_BASE_URL}")
    private String backendBaseUrl;

    public void updateSummary(Long documentId, String summary) {
        String url = backendBaseUrl + "/api/documents/" + documentId + "/summary";

        log.info("Updating document summary in backend: documentId={}, url={}", documentId, url);

        Map<String, String> body = Map.of("summary", summary);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            // ⬇️ Statt PATCH jetzt ein einfacher POST
            ResponseEntity<Void> response = restTemplate.postForEntity(url, entity, Void.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Backend summary update failed, status={}", response.getStatusCode());
                throw new IllegalStateException("Backend summary update failed");
            }

            log.info("Backend summary update successful for documentId={}", documentId);
        } catch (Exception e) {
            log.error("Error updating summary in backend for documentId={}", documentId, e);
            throw new IllegalStateException("Failed to update backend summary", e);
        }
    }
}
