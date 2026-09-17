package com.hassanusman.pulse.worker.metrics;

import com.hassanusman.pulse.contracts.RpcHeaders;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exposes broker queue depth so HPA/KEDA (or a custom scaler) can add consumers
 * when RPC wait time is dominated by queueing rather than compute.
 */
@Component
public class QueueDepthMetrics {

    private final AmqpAdmin amqpAdmin;
    private final AtomicInteger depth = new AtomicInteger();
    private final AtomicInteger consumers = new AtomicInteger();

    public QueueDepthMetrics(AmqpAdmin amqpAdmin, MeterRegistry registry) {
        this.amqpAdmin = amqpAdmin;
        Gauge.builder("pulse.rpc.queue.depth", depth, AtomicInteger::get)
                .description("Ready messages on the enrichment RPC queue")
                .register(registry);
        Gauge.builder("pulse.rpc.queue.consumers", consumers, AtomicInteger::get)
                .description("Consumers attached to the enrichment RPC queue")
                .register(registry);
    }

    @Scheduled(fixedDelayString = "${pulse.worker.queue-metric-ms:2000}")
    public void sample() {
        QueueInformation info = amqpAdmin.getQueueInfo(RpcHeaders.QUEUE);
        if (info == null) {
            return;
        }
        depth.set(info.getMessageCount());
        consumers.set(info.getConsumerCount());
    }

    public int currentDepth() {
        return depth.get();
    }
}
