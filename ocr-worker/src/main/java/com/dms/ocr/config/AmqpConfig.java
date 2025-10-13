package com.dms.ocr.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmqpConfig {

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        // Ensures RabbitMQ messages are serialized/deserialized as JSON
        return new Jackson2JsonMessageConverter();
    }
    @Bean
    public Queue documentUploadedQueue() {
        return new Queue("dms.document.uploaded", true);
    }
}
