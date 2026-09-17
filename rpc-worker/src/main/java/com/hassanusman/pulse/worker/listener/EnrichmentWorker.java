package com.hassanusman.pulse.worker.listener;

import com.hassanusman.pulse.contracts.EnrichmentRequest;
import com.hassanusman.pulse.contracts.EnrichmentResponse;
import com.hassanusman.pulse.contracts.RpcHeaders;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class EnrichmentWorker {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentWorker.class);

    private final Timer processTimer;
    private final Counter processed;
    private final long simulatedWorkMs;

    public EnrichmentWorker(MeterRegistry registry,
                            @Value("${pulse.worker.simulated-work-ms:25}") long simulatedWorkMs) {
        this.processTimer = Timer.builder("pulse.rpc.worker.process").publishPercentileHistogram().register(registry);
        this.processed = registry.counter("pulse.rpc.worker.processed");
        this.simulatedWorkMs = simulatedWorkMs;
    }

    @RabbitListener(queues = RpcHeaders.QUEUE)
    public EnrichmentResponse handle(EnrichmentRequest request, Message raw) {
        long deaths = deathCount(raw);
        if (deaths >= 3) {
            throw new AmqpRejectAndDontRequeueException("Exceeded retry budget, parking in DLQ path");
        }
        return processTimer.record(() -> process(request));
    }

    public EnrichmentResponse process(EnrichmentRequest request) {
        long start = System.nanoTime();
        sleepQuietly(simulatedWorkMs);
        List<String> ranked = new ArrayList<>(request.feedItemIds());
        ranked.sort(Comparator.naturalOrder());
        int score = Math.abs((request.memberId() + request.tenantId()).hashCode()) % 100;
        String summary = "Enriched " + ranked.size() + " items for " + request.memberId()
                + " blobChars=" + (request.verboseBlob() == null ? 0 : request.verboseBlob().length());
        processed.increment();
        long millis = (System.nanoTime() - start) / 1_000_000;
        log.debug("enriched requestId={} score={} workerMs={}", request.requestId(), score, millis);
        return new EnrichmentResponse(request.requestId(), request.memberId(), score, ranked, summary, Instant.now(), millis);
    }

    @SuppressWarnings("unchecked")
    private long deathCount(Message raw) {
        Object header = raw.getMessageProperties().getHeaders().get("x-death");
        if (header instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof java.util.Map<?, ?> map) {
            Object count = map.get("count");
            if (count instanceof Number n) {
                return n.longValue();
            }
        }
        return 0;
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
