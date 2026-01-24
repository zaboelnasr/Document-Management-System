package com.dms.listener;

import com.dms.dto.DocumentIndexDTO;
import com.dms.dto.OcrResultEvent;
import com.dms.service.IndexingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrResultListener {

    private final IndexingService indexingService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @RabbitListener(queues = "${dms.rmq.queue.ocr-result}")
    public void onOcrResult(String messageJson) throws Exception {
        OcrResultEvent event = objectMapper.readValue(messageJson, OcrResultEvent.class);
        log.info("Received OCR result for document id={}", event.documentId());

        DocumentIndexDTO doc = new DocumentIndexDTO(
                event.documentId(),
                event.fileName(),
                event.text(),
                event.summary(),
                event.createdAt(),
                event.reviewStatus()
        );

        indexingService.index(doc);
    }
}
