# ADR 0004 — Virtual threads on the RPC wait path

## Status
Accepted

## Context
Each HTTP request blocks until the worker replies. With platform threads, 50k concurrent in-flight RPCs is not a thread-pool problem you can buy your way out of.

## Decision
Enable `spring.threads.virtual.enabled=true` on the gateway. Keep a hard `pulse.rpc.max-in-flight` semaphore so we still shed load with 429 instead of unbounded memory.

## Consequences
- Worker listener threads remain AMQP container threads (bounded by `maxConcurrentConsumers`).
- Heap and broker unacked messages become the real ceilings, which we observe via Micrometer.
