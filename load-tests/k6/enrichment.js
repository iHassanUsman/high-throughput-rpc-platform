import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '1m', target: 200 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<2000'],
  },
};

export default function () {
  const res = http.post(
    `${__ENV.BASE_URL || 'http://localhost:8080'}/api/v1/enrichment`,
    JSON.stringify({ tenantId: 'acme', memberId: `m-${__VU}-${__ITER}` }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(0.05);
}
