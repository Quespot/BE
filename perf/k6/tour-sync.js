// TourAPI 동기화(POST /api/admin/tour-sync) 1회 실행 시간 측정
// 수집 단계는 TOUR_API_DAILY_LIMIT=0으로 건너뛰고, 미리 넣어 둔 원본(03_seed_tour_raw.sql)으로
// 정제·미션 후보 생성 단계만 측정한다
// 환경변수: JWT_SECRET(필수), BASE_URL, ADMIN_USER_ID(Redis 세션이 등록된 사용자 ID)
import http from 'k6/http';
import { check } from 'k6';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const JWT_SECRET = __ENV.JWT_SECRET;
const ADMIN_USER_ID = __ENV.ADMIN_USER_ID || '1';

if (!JWT_SECRET) {
  throw new Error('JWT_SECRET 환경변수가 필요합니다. (perf/perf.env 값)');
}

export const options = {
  scenarios: {
    tour_sync: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '60m',
    },
  },
  summaryTrendStats: ['avg', 'min', 'max'],
};

// JwtTokenProvider와 동일한 클레임의 ADMIN 액세스 토큰 (sid 세션은 seed/02_seed_sessions.sh로 등록)
function adminToken() {
  const now = Math.floor(Date.now() / 1000);
  const header = encoding.b64encode(JSON.stringify({ alg: 'HS256', typ: 'JWT' }), 'rawurl');
  const payload = encoding.b64encode(
    JSON.stringify({
      sub: String(ADMIN_USER_ID),
      jti: `k6-admin-${now}`,
      role: 'ADMIN',
      tokenType: 'ACCESS',
      sid: `k6-session-${ADMIN_USER_ID}`,
      iat: now,
      exp: now + 6 * 60 * 60,
    }),
    'rawurl',
  );
  const signature = crypto.hmac('sha256', encoding.b64decode(JWT_SECRET), `${header}.${payload}`, 'base64rawurl');
  return `${header}.${payload}.${signature}`;
}

export default function () {
  const res = http.post(`${BASE_URL}/api/admin/tour-sync`, null, {
    headers: { Authorization: `Bearer ${adminToken()}` },
    timeout: '60m',
  });

  check(res, {
    'status 200': (r) => r.status === 200,
    'isSuccess true': (r) => r.json('isSuccess') === true,
  });

  console.log(`tour-sync duration: ${(res.timings.duration / 1000).toFixed(2)}s, status: ${res.status}`);
  console.log(`result: ${JSON.stringify(res.json('result'))}`);
}
