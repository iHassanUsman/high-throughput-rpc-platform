package com.hassanusman.pulse.gateway.api;

import com.hassanusman.pulse.contracts.EnrichmentRequest;
import com.hassanusman.pulse.contracts.EnrichmentResponse;
import com.hassanusman.pulse.gateway.rpc.EnrichmentRpcClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/enrichment", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnrichmentController {

    private final EnrichmentRpcClient rpcClient;

    public EnrichmentController(EnrichmentRpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public record EnrichmentHttpRequest(
            @NotBlank String tenantId,
            @NotBlank String memberId,
            List<String> feedItemIds
    ) {
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public EnrichmentResponse enrich(@Valid @RequestBody EnrichmentHttpRequest body) {
        List<String> items = body.feedItemIds() == null || body.feedItemIds().isEmpty()
                ? List.of("item-1", "item-2", "item-3", "item-4", "item-5")
                : body.feedItemIds();
        EnrichmentRequest rpc = EnrichmentRequest.of(UUID.randomUUID().toString(), body.tenantId(), body.memberId(), items);
        return rpcClient.enrich(rpc);
    }

    @GetMapping("/health-permits")
    public int healthPermits() {
        return rpcClient.availablePermits();
    }
}
