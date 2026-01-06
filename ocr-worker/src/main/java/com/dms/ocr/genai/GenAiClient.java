package com.dms.ocr.genai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GenAiClient {

    private final RestTemplate restTemplate;

    // from docker-compose:
    // GENAI_WORKER_URL=http://genai-worker:8090/api/genai/summarize
    @Value("${GENAI_WORKER_URL}")
    private String genAiWorkerUrl;

    public String getSummary(String text) {
        if (text == null || text.isBlank()) {
            log.info("No text provided for summary, skipping GenAI call");
            return null;
        }

        log.info("Calling GenAI worker at {} (textLength={})",
                genAiWorkerUrl, text.length());

        Map<String, String> body = Map.of("text", text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity =
                new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            genAiWorkerUrl,
                            entity,
                            String.class
                    );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("GenAI worker returned HTTP {} – skipping summary",
                        response.getStatusCode());
                return null;
            }

            if (response.getBody() == null || response.getBody().isBlank()) {
                log.warn("GenAI worker returned empty body – skipping summary");
                return null;
            }

            String summary = response.getBody().trim();
            log.info("GenAI summary received (length={})", summary.length());
            return summary;

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("GenAI quota exceeded (429). Skipping summary.");
            return null;

        } catch (HttpClientErrorException e) {
            log.warn("GenAI worker HTTP error {} – skipping summary",
                    e.getStatusCode());
            return null;

        } catch (RestClientException e) {
            log.warn("GenAI worker unreachable – skipping summary");
            return null;
        }
    }
}
