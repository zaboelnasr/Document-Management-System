package com.dms.schedule.controller;

import com.dms.schedule.service.XmlBatchProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private final XmlBatchProcessor xmlBatchProcessor;

    public BatchController(XmlBatchProcessor xmlBatchProcessor) {
        this.xmlBatchProcessor = xmlBatchProcessor;
    }

    @PostMapping("/run")
    public ResponseEntity<XmlBatchProcessor.BatchResult> runBatch() {
        XmlBatchProcessor.BatchResult result = xmlBatchProcessor.processXmlBatch();
        return ResponseEntity.ok(result);
    }
}
