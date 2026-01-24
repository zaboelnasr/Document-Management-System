package com.dms.schedule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScheduleWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScheduleWorkerApplication.class, args);
    }
}
