# ADR 0003 — Scale consumers on queue depth, not only CPU

## Status
Accepted

## Context
When simulated (or real) enrichment work is I/O-ish, CPU-based HPA lags. Messages pile up, RPC wait time explodes, and the API looks "down" while pods are idle-looking.

## Decision
1. In-process: `concurrentConsumers` / `maxConcurrentConsumers` plus a conservative `prefetch` (10) so one slow consumer cannot hoard the queue.
2. In-cluster: KEDA `ScaledObject` on `pulse.enrichment.rpc` queue length, plus CPU HPA on the gateway.
3. Export `pulse.rpc.queue.depth` for Grafana.

Prefetch that is too high was a major contributor to 20s+ API times in similar systems: workers looked busy while other consumers starved.
