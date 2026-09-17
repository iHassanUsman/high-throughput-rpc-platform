# ADR 0001 — Gzip JSON over AMQP instead of Protobuf

## Status
Accepted

## Context
RPC payloads for enrichment carry a large contextual blob (tenant flags, feed metadata). Uncompressed JSON saturated NIC and inflated RPC wait time under concurrent load. Protobuf would shrink payloads further but requires a schema pipeline and client codegen.

## Decision
Keep JSON for human-debuggability on the RabbitMQ management UI, and gzip the body when it exceeds a byte threshold. Skip gzip for tiny messages so CPU is not wasted on incompressible envelopes.

## Consequences
- Broker and packet capture show `contentEncoding=gzip` plus `x-uncompressed-size`.
- Workers and the gateway share one converter (`GzipJacksonMessageConverter`).
- Switching to Protobuf later is additive: keep the same RPC topology.
