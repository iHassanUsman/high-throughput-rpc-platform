package com.hassanusman.pulse.worker;

import com.hassanusman.pulse.contracts.EnrichmentRequest;
import com.hassanusman.pulse.contracts.EnrichmentResponse;
import com.hassanusman.pulse.worker.listener.EnrichmentWorker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrichmentWorkerTest {

    @Test
    void ranksFeedAndKeepsRequestId() {
        EnrichmentWorker worker = new EnrichmentWorker(new SimpleMeterRegistry(), 0);
        EnrichmentRequest request = EnrichmentRequest.of("req-9", "acme", "member-1", List.of("c", "a", "b"));
        EnrichmentResponse response = worker.process(request);
        assertEquals("req-9", response.requestId());
        assertEquals(List.of("a", "b", "c"), response.rankedFeedItemIds());
        assertEquals("member-1", response.memberId());
        assertTrue(response.score() >= 0 && response.score() < 100);
    }
}
