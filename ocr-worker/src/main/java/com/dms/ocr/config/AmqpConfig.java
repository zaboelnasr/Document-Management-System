package com.dms.ocr.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AmqpConfig {

    @Value("${dms.rmq.exchange:dms.exchange}")
    private String exchangeName;

    @Value("${dms.rmq.queue.upload:dms.document.uploaded}")
    private String uploadQueueName;

    @Value("${dms.rmq.routing.upload:dms.document.uploaded}")
    private String uploadRoutingKey;

    @Bean
    public TopicExchange dmsExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue uploadQueue() {
        return new Queue(uploadQueueName, true);
    }

    @Bean
    public Binding uploadBinding(Queue uploadQueue, TopicExchange dmsExchange) {
        return BindingBuilder.bind(uploadQueue).to(dmsExchange).with(uploadRoutingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
