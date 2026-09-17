package com.hassanusman.pulse.contracts;

import java.time.Instant;
import java.util.List;

public record EnrichmentResponse(
        String requestId,
        String memberId,
        int score,
        List<String> rankedFeedItemIds,
        String summary,
        Instant completedAt,
        long workerMillis
) {
}
