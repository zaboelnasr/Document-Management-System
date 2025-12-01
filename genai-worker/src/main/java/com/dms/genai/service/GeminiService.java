package com.dms.genai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final RestTemplate restTemplate;

    @Value("${genai.api-key}")
    private String apiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    public String summarize(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY / genai.api-key is not configured");
        }

        String prompt =
                "Fasse den folgenden deutschen Text in 2–3 Sätzen kurz und verständlich zusammen. " +
                        "Gib NUR die Zusammenfassung aus, ohne Einleitung, ohne Erklärungen, ohne Bulletpoints.\n\n"
                        + text;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = GEMINI_URL + apiKey;

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Gemini API returned status " + response.getStatusCode());
            }

            Map<String, Object> respBody = response.getBody();
            if (respBody == null) {
                throw new IllegalStateException("Gemini API returned empty body");
            }

            // candidates[0].content.parts[0].text
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) respBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new IllegalStateException("No candidates in Gemini response");
            }

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) {
                throw new IllegalStateException("No content in first candidate");
            }

            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                throw new IllegalStateException("No parts in content");
            }

            Object textObj = parts.get(0).get("text");
            if (textObj == null) {
                throw new IllegalStateException("No text in first part");
            }

            String summary = textObj.toString().trim();
            log.info("Gemini summary length={}", summary.length());
            return summary;

        } catch (RestClientException e) {
            log.error("HTTP error calling Gemini API", e);
            throw new IllegalStateException("Failed to call Gemini API", e);
        } catch (ClassCastException e) {
            log.error("Unexpected response structure from Gemini API", e);
            throw new IllegalStateException("Failed to parse Gemini response", e);
        }
    }
}