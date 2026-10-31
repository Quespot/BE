// 거리순 미션 목록 API 부하 테스트 (GET /api/missions, 사용자당 최대 PAGES 페이지 조회)
// 개선 전후 동일 부하 비교를 위해 고정 도착률(constant-arrival-rate) 사용
// 환경변수: JWT_SECRET(필수), BASE_URL, RATE(초당 시나리오 수), DURATION, PAGES, USER_COUNT
import http from 'k6/http';
import { check } from 'k6';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const JWT_SECRET = __ENV.JWT_SECRET;
const RATE = Number(__ENV.RATE || 20);
const DURATION = __ENV.DURATION || '2m';
const PAGES = Number(__ENV.PAGES || 3);
const USER_COUNT = Number(__ENV.USER_COUNT || 1000);

if (!JWT_SECRET) {
  throw new Error('JWT_SECRET 환경변수가 필요합니다. (perf/perf.env 값)');
}

const firstPageDuration = new Trend('mission_list_first_page', true);
const nextPageDuration = new Trend('mission_list_next_page', true);

export const options = {
  scenarios: {
    distance_list: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Math.max(20, RATE * 2),
      maxVUs: Math.max(200, RATE * 10),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// 사용자 위치: 도시 중심 기준 약 ±5km 범위
const CITIES = [
  { lat: 37.5665, lng: 126.9780, weight: 6 }, // 서울
  { lat: 35.1796, lng: 129.0756, weight: 3 }, // 부산
  { lat: 37.4563, lng: 126.7052, weight: 2 }, // 인천
  { lat: 35.8714, lng: 128.6014, weight: 2 }, // 대구
  { lat: 36.3504, lng: 127.3845, weight: 1 }, // 대전
  { lat: 35.1595, lng: 126.8526, weight: 1 }, // 광주
  { lat: 33.4996, lng: 126.5312, weight: 2 }, // 제주
  { lat: 37.7519, lng: 128.8761, weight: 1 }, // 강릉
  { lat: 35.8562, lng: 129.2247, weight: 1 }, // 경주
  { lat: 35.8242, lng: 127.1480, weight: 1 }, // 전주
];
const WEIGHTED = CITIES.flatMap((c) => Array(c.weight).fill(c));

function randomLocation() {
  const city = WEIGHTED[Math.floor(Math.random() * WEIGHTED.length)];
  const jitter = () => (Math.random() - 0.5) * 0.09;
  return {
    latitude: (city.lat + jitter()).toFixed(6),
    longitude: (city.lng + jitter()).toFixed(6),
  };
}

// JwtTokenProvider와 동일한 클레임의 HS256 액세스 토큰 생성 (sid 세션은 seed/02_seed_sessions.sh로 Redis에 등록)
const SIGNING_KEY = encoding.b64decode(JWT_SECRET);
const tokenCache = {};

function accessToken(userId) {
  if (tokenCache[userId]) {
    return tokenCache[userId];
  }
  const now = Math.floor(Date.now() / 1000);
  const header = encoding.b64encode(JSON.stringify({ alg: 'HS256', typ: 'JWT' }), 'rawurl');
  const payload = encoding.b64encode(
    JSON.stringify({
      sub: String(userId),
      jti: `k6-${userId}-${now}`,
      role: 'USER',
      tokenType: 'ACCESS',
      sid: `k6-session-${userId}`,
      iat: now,
      exp: now + 6 * 60 * 60,
    }),
    'rawurl',
  );
  const signature = crypto.hmac('sha256', SIGNING_KEY, `${header}.${payload}`, 'base64rawurl');
  const token = `${header}.${payload}.${signature}`;
  tokenCache[userId] = token;
  return token;
}

export default function () {
  const userId = 1 + Math.floor(Math.random() * USER_COUNT);
  const params = {
    headers: { Authorization: `Bearer ${accessToken(userId)}` },
  };
  const { latitude, longitude } = randomLocation();

  let cursor = null;
  for (let page = 1; page <= PAGES; page++) {
    let url = `${BASE_URL}/api/missions?latitude=${latitude}&longitude=${longitude}&size=20`;
    if (cursor) {
      url += `&cursor=${encodeURIComponent(cursor)}`;
    }
    const tag = page === 1 ? 'first' : 'next';
    const res = http.get(url, Object.assign({}, params, { tags: { name: 'GET /api/missions', page: tag } }));

    const ok = check(res, {
      'status 200': (r) => r.status === 200,
      'isSuccess true': (r) => r.json('isSuccess') === true,
    });
    (page === 1 ? firstPageDuration : nextPageDuration).add(res.timings.duration);

    if (!ok) {
      break;
    }
    const result = res.json('result');
    if (!result || !result.hasNext) {
      break;
    }
    cursor = result.nextCursor;
  }
}
