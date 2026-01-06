package com.dms.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${dms.rmq.exchange}")
    private String exchange;

    @Value("${dms.rmq.queue.index}")
    private String indexQueue;

    @Value("${dms.rmq.routing.index}")
    private String routingKey;

    @Bean
    public TopicExchange dmsExchange() {
        return ExchangeBuilder.topicExchange(exchange).durable(true).build();
    }

    @Bean
    public Queue indexQueue() {
        return QueueBuilder.durable(indexQueue).build();
    }

    @Bean
    public Binding indexBinding() {
        return BindingBuilder
                .bind(indexQueue())
                .to(dmsExchange())
                .with(routingKey);
    }
}
