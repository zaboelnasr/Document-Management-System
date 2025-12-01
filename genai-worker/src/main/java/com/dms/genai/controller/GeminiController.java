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
        String text = req != null ? req.getText() : null;
        log.info("GenAI request received, textLength={}", text != null ? text.length() : 0);

        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body("text must not be empty");
        }

        try {
            String summary = geminiService.summarize(text);
            // ⚠️ WICHTIG: wir geben NUR den reinen Text zurück
            return ResponseEntity.ok(summary);
        } catch (IllegalStateException e) {
            log.error("Configuration or Gemini error", e);
            return ResponseEntity.status(502).body("Summary generation failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during summarization", e);
            return ResponseEntity.status(502).body("Summary generation failed");
        }
    }
}