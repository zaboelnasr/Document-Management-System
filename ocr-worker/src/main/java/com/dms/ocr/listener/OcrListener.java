package com.dms.ocr.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OcrListener {

    private static final Logger log = LoggerFactory.getLogger(OcrListener.class);

    /**
     * Listens to document upload events.
     * The queue name comes from the environment variable DMS_RMQ_QUEUE_UPLOAD.
     */
    @RabbitListener(queues = "${DMS_RMQ_QUEUE_UPLOAD:dms.document.uploaded}")
    public void onDocumentUploaded(Map<String, Object> event) {
        log.info("[OCR-WORKER] Received document upload event: {}", event);

        // TODO: Future OCR logic goes here
        // For now, this worker just logs the event to confirm queue connection
    }
}
