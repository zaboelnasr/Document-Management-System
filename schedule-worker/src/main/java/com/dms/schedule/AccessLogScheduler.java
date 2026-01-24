package com.dms.schedule;

import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AccessLogScheduler {

    @Scheduled(fixedRate = 60_000) // every 1 minute
    public void run() {
        System.out.println("✅ Schedule worker running at " + LocalDateTime.now());

        // TODO: call service
        // accessLogService.processLogs();
    }
}
