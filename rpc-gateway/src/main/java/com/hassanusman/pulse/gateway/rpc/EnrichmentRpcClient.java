package com.hassanusman.pulse.gateway.rpc;

import com.hassanusman.pulse.contracts.EnrichmentRequest;
import com.hassanusman.pulse.contracts.EnrichmentResponse;
import com.hassanusman.pulse.contracts.RpcHeaders;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

/**
 * Blocking AMQP RPC on a virtual thread. The servlet/tomcat pool is not exhausted
 * because {@code spring.threads.virtual.enabled=true}.
 */
@Service
public class EnrichmentRpcClient {

    private final RabbitTemplate rabbitTemplate;
    private final Timer rpcTimer;
    private final Semaphore inFlight;

    public EnrichmentRpcClient(RabbitTemplate rabbitTemplate,
                               MeterRegistry registry,
                               org.springframework.core.env.Environment env) {
        this.rabbitTemplate = rabbitTemplate;
        this.rpcTimer = Timer.builder("pulse.rpc.client.latency")
                .publishPercentileHistogram()
                .register(registry);
        int maxInFlight = env.getProperty("pulse.rpc.max-in-flight", Integer.class, 2000);
        this.inFlight = new Semaphore(maxInFlight);
    }

    @TimeLimiter(name = "enrichmentRpc")
    public CompletableFuture<EnrichmentResponse> enrichAsync(EnrichmentRequest request) {
        return CompletableFuture.supplyAsync(() -> enrich(request));
    }

    public EnrichmentResponse enrich(EnrichmentRequest request) {
        if (!inFlight.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "RPC backpressure: too many in-flight requests");
        }
        try {
            return rpcTimer.record(() -> doRpc(request));
        } finally {
            inFlight.release();
        }
    }

    private EnrichmentResponse doRpc(EnrichmentRequest request) {
        String correlationId = request.requestId() == null ? UUID.randomUUID().toString() : request.requestId();
        MessagePostProcessor headers = message -> {
            message.getMessageProperties().setCorrelationId(correlationId);
            message.getMessageProperties().setHeader(RpcHeaders.CORRELATION_ID, correlationId);
            message.getMessageProperties().setHeader(RpcHeaders.TENANT_ID, request.tenantId());
            return message;
        };
        try {
            Object reply = rabbitTemplate.convertSendAndReceive(
                    RpcHeaders.EXCHANGE,
                    RpcHeaders.ROUTING_KEY,
                    request,
                    headers);
            if (reply == null) {
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "RPC timed out waiting for worker");
            }
            return (EnrichmentResponse) reply;
        } catch (AmqpException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Broker unavailable: " + ex.getMessage(), ex);
        }
    }

    public int availablePermits() {
        return inFlight.availablePermits();
    }
}
