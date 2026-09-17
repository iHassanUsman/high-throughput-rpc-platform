package com.hassanusman.pulse.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hassanusman.pulse.contracts.GzipJacksonMessageConverter;
import com.hassanusman.pulse.contracts.RpcHeaders;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerRabbitConfig {

    @Value("${pulse.worker.concurrent-consumers:4}")
    private int concurrentConsumers;

    @Value("${pulse.worker.max-concurrent-consumers:32}")
    private int maxConcurrentConsumers;

    @Value("${pulse.worker.prefetch:10}")
    private int prefetch;

    @Value("${pulse.rpc.gzip-threshold-bytes:512}")
    private int gzipThreshold;

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
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter gzipMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(gzipMessageConverter);
        factory.setConcurrentConsumers(concurrentConsumers);
        factory.setMaxConcurrentConsumers(maxConcurrentConsumers);
        factory.setPrefetchCount(prefetch);
        factory.setDefaultRequeueRejected(false);
        factory.setObservationEnabled(true);
        factory.setMissingQueuesFatal(false);
        return factory;
    }
}
