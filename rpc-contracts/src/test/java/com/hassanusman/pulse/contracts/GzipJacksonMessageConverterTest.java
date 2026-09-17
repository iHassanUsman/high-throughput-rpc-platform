package com.hassanusman.pulse.contracts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GzipJacksonMessageConverterTest {

    private final GzipJacksonMessageConverter converter =
            new GzipJacksonMessageConverter(new ObjectMapper(), 256);

    @Test
    void compressesLargePayloadAndRoundTrips() {
        EnrichmentRequest request = EnrichmentRequest.of("r-1", "tenant-a", "member-9", List.of("f1", "f2", "f3"));
        Message encoded = converter.toMessage(request, new MessageProperties());

        assertEquals(RpcHeaders.COMPRESSED, encoded.getMessageProperties().getContentEncoding());
        int uncompressed = (Integer) encoded.getMessageProperties().getHeader(RpcHeaders.UNCOMPRESSED_SIZE);
        assertTrue(encoded.getBody().length < uncompressed);

        EnrichmentRequest decoded = (EnrichmentRequest) converter.fromMessage(encoded);
        assertEquals(request.requestId(), decoded.requestId());
        assertEquals(request.verboseBlob(), decoded.verboseBlob());
        assertEquals(request.feedItemIds(), decoded.feedItemIds());
    }

    @Test
    void skipsGzipForTinyPayloads() {
        EnrichmentRequest tiny = new EnrichmentRequest("r", "t", "m", List.of("a"), java.util.Map.of(), "x");
        Message encoded = converter.toMessage(tiny, new MessageProperties());
        assertNotEquals(RpcHeaders.COMPRESSED, encoded.getMessageProperties().getContentEncoding());
        EnrichmentRequest decoded = (EnrichmentRequest) converter.fromMessage(encoded);
        assertEquals("r", decoded.requestId());
    }
}
