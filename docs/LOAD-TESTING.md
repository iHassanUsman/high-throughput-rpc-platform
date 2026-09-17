# Load-testing methodology

This repository does **not** ship a fabricated 50k-concurrent result. Reproduce on hardware you control.

## What to measure

| Signal | Where | Healthy |
| --- | --- | --- |
| HTTP p95 | k6 | under your SLO (example 2s) |
| `pulse.rpc.client.latency` | Prometheus | tracks HTTP closely |
| `pulse.rpc.queue.depth` | Prometheus | should not monotonically grow |
| Gzip ratio | `pulse.rpc.gzip.bytes.saved` | large payloads should compress |
| 429/504 rate | k6 + gateway logs | backpressure working |

## Recipe

1. `mvn -q -DskipTests package`
2. `docker compose up --build`
3. `k6 run load-tests/k6/enrichment.js`
4. Raise `WORKER_CONCURRENT` and add worker replicas; repeat.
5. Compare runs with `PULSE_RPC_GZIP_THRESHOLD_BYTES` set very high (effectively off) versus 512.

## Levers that historically dropped multi-tens-of-seconds API times

1. Gzip the RPC body (NIC and serialization).
2. Lower prefetch so consumers share work.
3. Scale consumers on queue depth (KEDA), not only CPU.
4. Do not block a small platform-thread HTTP pool on RPC (virtual threads + in-flight cap).
