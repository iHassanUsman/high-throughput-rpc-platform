package com.hassanusman.pulse.gateway;

import com.hassanusman.pulse.contracts.EnrichmentResponse;
import com.hassanusman.pulse.gateway.rpc.EnrichmentRpcClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.hassanusman.pulse.gateway.api.EnrichmentController.class)
@Import(com.hassanusman.pulse.gateway.api.ApiExceptionHandler.class)
class EnrichmentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EnrichmentRpcClient rpcClient;

    @Test
    void postsEnrichmentRequest() throws Exception {
        Mockito.when(rpcClient.enrich(any())).thenReturn(
                new EnrichmentResponse("id", "m1", 87, List.of("a"), "ok", Instant.parse("2026-01-01T00:00:00Z"), 12)
        );

        mockMvc.perform(post("/api/v1/enrichment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"t1\",\"memberId\":\"m1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(87))
                .andExpect(jsonPath("$.memberId").value("m1"));
    }

    @Test
    void rejectsBlankTenant() throws Exception {
        mockMvc.perform(post("/api/v1/enrichment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"\",\"memberId\":\"m1\"}"))
                .andExpect(status().isBadRequest());
    }
}
