package com.dms.schedule;

import com.dms.schedule.service.XmlBatchProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AccessLogScheduler {

    private static final Logger log = LoggerFactory.getLogger(AccessLogScheduler.class);
    private final XmlBatchProcessor xmlBatchProcessor;

    public AccessLogScheduler(XmlBatchProcessor xmlBatchProcessor) {
        this.xmlBatchProcessor = xmlBatchProcessor;
    }

    @Scheduled(cron = "${schedule.cron:0 0 1 * * *}")
    public void run() {
        log.info("Schedule worker running at {}", LocalDateTime.now());
        xmlBatchProcessor.processXmlBatch();
    }
}
