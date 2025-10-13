package com.dms.ocr.listener;

import com.dms.ocr.event.DocumentUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OcrListener {

    private static final Logger log = LoggerFactory.getLogger(OcrListener.class);

    @RabbitListener(queues = "${dms.rmq.queue.upload}")
    public void handleUpload(DocumentUploadedEvent event) {
        log.info("[OCR-WORKER] Received DocumentUploadedEvent: id={}, fileName={}",
                event.getId(), event.getFileName());
    }
}
