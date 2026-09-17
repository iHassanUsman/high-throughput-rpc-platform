package com.hassanusman.pulse.contracts;

public final class RpcHeaders {

    public static final String CORRELATION_ID = "x-correlation-id";
    public static final String TENANT_ID = "x-tenant-id";
    public static final String UNCOMPRESSED_SIZE = "x-uncompressed-size";
    public static final String COMPRESSED = "gzip";
    public static final String CONTENT_ENCODING = "contentEncoding";

    public static final String EXCHANGE = "pulse.rpc";
    public static final String ROUTING_KEY = "enrichment";
    public static final String QUEUE = "pulse.enrichment.rpc";
    public static final String RETRY_QUEUE = "pulse.enrichment.retry";
    public static final String DLQ = "pulse.enrichment.dlq";

    private RpcHeaders() {
    }
}
