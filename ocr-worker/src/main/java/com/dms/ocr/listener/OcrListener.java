package com.dms.ocr.listener;

import com.dms.ocr.backend.BackendClient;
import com.dms.ocr.event.DocumentUploadedEvent;
import com.dms.ocr.event.OcrResultEvent;
import com.dms.ocr.genai.GenAiClient;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class OcrListener {

    private static final Logger log = LoggerFactory.getLogger(OcrListener.class);

    private final S3Client s3;
    private final GenAiClient genAiClient;
    private final BackendClient backendClient;
    private final RabbitTemplate rabbitTemplate;

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

        // 2) render PDF → PNG
        Path outDir = Files.createTempDirectory("ocr-" + event.getId());
        Process gs = new ProcessBuilder(
                "gs", "-dSAFER", "-dBATCH", "-dNOPAUSE",
                "-sDEVICE=png16m", "-r300",
                "-sOutputFile=" + outDir.resolve("page-%03d.png"),
                pdf.toString()
        ).redirectErrorStream(true).start();

        if (gs.waitFor() != 0) {
            log.error("Ghostscript failed for doc id={}", event.getId());
            return;
        }

        // 3) OCR
        StringBuilder all = new StringBuilder();
        try (Stream<Path> files = Files.list(outDir)) {
            List<Path> pages = files
                    .filter(p -> p.getFileName().toString().endsWith(".png"))
                    .sorted()
                    .toList();

            for (Path page : pages) {
                Process p = new ProcessBuilder(
                        "tesseract", page.toString(), "stdout", "-l", "eng+deu"
                ).redirectErrorStream(true).start();

                try (BufferedReader br =
                             new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    br.lines().forEach(line -> all.append(line).append('\n'));
                }
                p.waitFor();
            }
        }

        String text = all.toString().trim();

        log.info("OCR completed for document id={}, textLength={}",
                event.getId(), text.length());

        if (text.isEmpty()) {
            log.warn("No text detected for document id={}", event.getId());
            return;
        }

        // 4) GenAI summary (best effort)
        String summary = null;
        try {
            summary = genAiClient.getSummary(text);
        } catch (Exception e) {
            log.warn("GenAI failed for document id={}, continuing without summary", event.getId());
        }

        // 5) Update backend summary (optional)
        if (summary != null) {
            try {
                backendClient.updateSummary(event.getId(), summary);
            } catch (Exception e) {
                log.error("Failed to update backend summary for id={}", event.getId(), e);
            }
        }

        // 6) 🚀 SEND OCR RESULT TO INDEXING WORKER
        rabbitTemplate.convertAndSend(
                exchange,
                ocrResultRoutingKey,
                new OcrResultEvent(
                        event.getId(),
                        event.getFileName(),
                        text,
                        summary
                )
        );

        log.info("Published OcrResultEvent for document id={}", event.getId());
    }

    private static String getSafe(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
