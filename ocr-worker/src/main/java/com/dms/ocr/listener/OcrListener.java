package com.dms.ocr.listener;

import com.dms.ocr.backend.BackendClient;
import com.dms.ocr.event.DocumentUploadedEvent;
import com.dms.ocr.event.OcrResultEvent;
import com.dms.ocr.genai.GenAiClient;
import com.dms.ocr.service.OcrEngine;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.file.*;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OcrListener {

    private static final Logger log = LoggerFactory.getLogger(OcrListener.class);

    private final S3Client s3;
    private final GenAiClient genAiClient;
    private final BackendClient backendClient;
    private final RabbitTemplate rabbitTemplate;
    private final OcrEngine ocrEngine;

    @Value("${s3.bucket}")
    private String defaultBucket;

    @Value("${dms.rmq.exchange}")
    private String exchange;

    @Value("${dms.rmq.routing.ocr-result}")
    private String ocrResultRoutingKey;

    // listens to upload event
    @RabbitListener(queues = "${dms.rmq.queue.upload}")
    public void onMessage(DocumentUploadedEvent event) throws Exception {

        String bucket = Optional
                .ofNullable(getSafe(event.getBucket(), defaultBucket))
                .orElse(defaultBucket);

        String key = event.getObjectKey();

        if (key == null || key.isBlank()) {
            log.error("Missing objectKey for document id={}", event.getId());
            return;
        }

        // 1) download PDF
        Path pdf = Files.createTempFile("doc-" + event.getId(), ".pdf");
        try (ResponseInputStream<GetObjectResponse> in =
                     s3.getObject(b -> b.bucket(bucket).key(key))) {
            Files.copy(in, pdf, StandardCopyOption.REPLACE_EXISTING);
        }

        // 2) OCR
        String text = ocrEngine.extractText(pdf, event.getId());

        log.info("OCR completed for document id={}, textLength={}",
                event.getId(), text.length());

        if (text.isEmpty()) {
            log.warn("No text detected for document id={}", event.getId());
            return;
        }

        // 3) GenAI summary (best effort)
        String summary = null;
        try {
            summary = genAiClient.getSummary(text);
        } catch (Exception e) {
            log.warn("GenAI failed for document id={}, continuing without summary", event.getId());
        }

        // 4) Update backend summary (optional)
        if (summary != null) {
            try {
                backendClient.updateSummary(event.getId(), summary);
            } catch (Exception e) {
                log.error("Failed to update backend summary for id={}", event.getId(), e);
            }
        }

        // 5) 🚀 SEND OCR RESULT TO INDEXING WORKER
        rabbitTemplate.convertAndSend(
                exchange,
                ocrResultRoutingKey,
                new OcrResultEvent(
                        event.getId(),
                        event.getFileName(),
                        text,
                        summary,
                        event.getCreatedAt(),
                        event.getReviewStatus()
                )

        );

        log.info("Published OcrResultEvent for document id={}", event.getId());
    }

    private static String getSafe(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
