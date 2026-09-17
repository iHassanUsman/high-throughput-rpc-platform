package com.hassanusman.pulse.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hassanusman.pulse.contracts.GzipJacksonMessageConverter;
import com.hassanusman.pulse.contracts.RpcHeaders;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RabbitRpcConfig {

    @Value("${pulse.rpc.gzip-threshold-bytes:512}")
    private int gzipThreshold;

    @Value("${pulse.rpc.timeout:4s}")
    private Duration rpcTimeout;

    @Bean
    MessageConverter gzipMessageConverter(ObjectMapper objectMapper, MeterRegistry registry) {
        return new GzipJacksonMessageConverter(objectMapper, gzipThreshold, registry);
    }

    @Bean
    DirectExchange pulseExchange() {
        return new DirectExchange(RpcHeaders.EXCHANGE, true, false);
    }

    @Bean
    Queue enrichmentQueue() {
        return QueueBuilder.durable(RpcHeaders.QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", RpcHeaders.RETRY_QUEUE)
                .build();
    }

    @Bean
    Queue retryQueue() {
        return QueueBuilder.durable(RpcHeaders.RETRY_QUEUE)
                .withArgument("x-message-ttl", 5000)
                .withArgument("x-dead-letter-exchange", RpcHeaders.EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RpcHeaders.ROUTING_KEY)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(RpcHeaders.DLQ).build();
    }

    @Bean
    Binding enrichmentBinding(Queue enrichmentQueue, DirectExchange pulseExchange) {
        return BindingBuilder.bind(enrichmentQueue).to(pulseExchange).with(RpcHeaders.ROUTING_KEY);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter gzipMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(gzipMessageConverter);
        template.setExchange(RpcHeaders.EXCHANGE);
        template.setRoutingKey(RpcHeaders.ROUTING_KEY);
        template.setReplyTimeout(rpcTimeout.toMillis());
        template.setMandatory(true);
        template.setObservationEnabled(true);
        return template;
    }
}
