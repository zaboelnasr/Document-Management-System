package com.dms.genai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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
            log.warn("Gemini API key is not configured, skipping summary");
            return null;
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
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(url, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Gemini API returned non-2xx status: {}", response.getStatusCode());
                return null;
            }

            Map<String, Object> respBody = response.getBody();
            if (respBody == null) return null;

            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) respBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;

            Map<String, Object> content =
                    (Map<String, Object>) candidates.get(0).get("content");
            if (content == null) return null;

            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) return null;

            Object textObj = parts.get(0).get("text");
            if (textObj == null) return null;

            String summary = textObj.toString().trim();
            log.info("Gemini summary length={}", summary.length());
            return summary;

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Gemini quota exceeded (429). Skipping summary generation.");
            return null;

        } catch (HttpClientErrorException e) {
            log.error("Gemini API error: {}", e.getStatusCode());
            return null;

        } catch (RestClientException | ClassCastException e) {
            log.error("Failed to call or parse Gemini API response", e);
            return null;
        }
    }
}
