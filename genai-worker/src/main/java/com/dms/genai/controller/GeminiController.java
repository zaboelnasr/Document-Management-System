package com.dms.genai.controller;

import com.dms.genai.dto.SummaryRequest;
import com.dms.genai.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/genai")
@RequiredArgsConstructor
@Slf4j
public class GeminiController {

    private final GeminiService geminiService;

    @PostMapping("/summarize")
    public ResponseEntity<String> summarize(@RequestBody SummaryRequest req) {

        String text = req.getText();
        log.info("GenAI request received, textLength={}",
                text != null ? text.length() : 0);

        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body("text must not be empty");
        }

        try {
            // ⬇️ Unser Service liefert NUR den reinen Summary-Text zurück
            String summary = geminiService.summarize(text);

            // Sicherheit: falls Gemini zurückgibt was länger ist
            if (summary.length() > 255) {
                log.warn("Summary length {} truncated to 255 chars", summary.length());
                summary = summary.substring(0, 255);
            }

            return ResponseEntity.ok(summary);

        } catch (IllegalStateException e) {
            log.error("Configuration error in GenAI worker", e);
            return ResponseEntity.status(500)
                    .body("GenAI worker misconfigured: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error during summarization", e);
            return ResponseEntity.status(502)
                    .body("Summary generation failed");
        }
    }
}
