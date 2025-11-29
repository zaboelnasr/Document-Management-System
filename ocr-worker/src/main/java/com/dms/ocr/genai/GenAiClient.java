package com.dms.ocr.genai;

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
public class GenAiClient {

    private final RestTemplate restTemplate;

    // kommt aus docker-compose: GENAI_WORKER_URL: http://genai-worker:8090/api/genai/summarize
    @Value("${GENAI_WORKER_URL}")
    private String genAiWorkerUrl;

    public String getSummary(String text) {
        log.info("Calling GenAI worker at {} with textLength={}", genAiWorkerUrl, text.length());

        Map<String, String> body = Map.of("text", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(genAiWorkerUrl, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("GenAI worker returned non-2xx status={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new IllegalStateException("GenAI worker call failed");
            }

            String summary = response.getBody();
            log.info("GenAI worker call successful, summaryLength={}", summary.length());

            // aktuell ist das noch das JSON von Gemini – für den Sprint reicht das
            return summary;
        } catch (Exception e) {
            log.error("Error calling GenAI worker", e);
            throw new IllegalStateException("Failed to call GenAI worker", e);
        }
    }
}
