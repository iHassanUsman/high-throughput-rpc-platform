package com.hassanusman.pulse.contracts;

import java.util.List;
import java.util.Map;

/**
 * RPC request for Pulse feed/profile enrichment.
 * Payload is intentionally verbose so Gzip savings are measurable under load.
 */
public record EnrichmentRequest(
        String requestId,
        String tenantId,
        String memberId,
        List<String> feedItemIds,
        Map<String, String> context,
        String verboseBlob
) {
    public static EnrichmentRequest of(String requestId, String tenantId, String memberId, List<String> feedItemIds) {
        String blob = ("pulse-context|" + tenantId + "|" + memberId + "|").repeat(400);
        return new EnrichmentRequest(requestId, tenantId, memberId, feedItemIds, Map.of("channel", "web"), blob);
    }
}
