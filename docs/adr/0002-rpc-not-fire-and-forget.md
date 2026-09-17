# ADR 0002 — Synchronous RPC over RabbitMQ, not fire-and-forget

## Status
Accepted

## Context
The HTTP API must return an enriched document in the same request. Fire-and-forget plus polling would change the product contract and add a read-model.

## Decision
Use AMQP request/reply (`convertSendAndReceive`) with a correlation id and a 4s client timeout. HTTP threads are virtual threads so waiting on the broker does not exhaust Tomcat's platform-thread pool.

## Consequences
- Tail latency is dominated by queue wait + worker time, which is the correct signal for consumer autoscaling.
- Callers receive 504 on timeout and 429 when the in-flight semaphore is exhausted (backpressure).
