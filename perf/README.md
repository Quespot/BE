# Quespot 성능 테스트

운영 서버와 분리된 로컬 환경에서 같은 조건으로 **개선 전(Before)과 개선 후(After)**를 측정하기 위한 스크립트입니다.

```text
perf/
├── docker-compose.perf.yml   # 성능 테스트 전용 MySQL(3308)·Redis(6380)
├── perf.env                  # 로컬 더미 환경변수 (운영 값 아님)
├── seed/01_seed_missions.sql # 미션 10만 건 더미 데이터
├── seed/02_seed_sessions.sh  # k6 가상 사용자 로그인 세션(Redis) 등록
├── sql/explain_distance.sql  # 거리순 목록 쿼리 실행 계획(EXPLAIN ANALYZE)
├── k6/mission-distance.js    # 거리순 목록 API 부하 테스트
└── results/                  # 측정 결과 저장
```

## 측정 조건 (운영과 맞춘 값)

| 항목 | 운영 | 성능 테스트 |
| --- | --- | --- |
| 앱 힙 메모리 | 컨테이너 600MB × 60% ≈ 360MB | `-Xmx360m` |
| 커넥션 풀 | Hikari 5 | `DB_POOL_MAX_SIZE=5` |
| DB | RDS MySQL 8.x | MySQL 8.4 (Docker) |
| 데이터 | 미션 수십 건 | 미션 10만 건 (서비스 확장 가정) |

## 1. 인프라 실행

```bash
cd ~/project/QUESPOT_BACKEND
docker compose -f perf/docker-compose.perf.yml up -d
```

## 2. 앱 빌드·실행 (스키마 자동 생성)

```bash
./gradlew bootJar -x test
set -a; source perf/perf.env; set +a
java -Xms120m -Xmx360m -jar build/libs/quespot-0.0.1-SNAPSHOT.jar
```

`Started QuespotApplication` 로그가 보이면 Hibernate(`ddl-auto: update`)가 테이블을 만든 상태입니다. 앱은 켜 둔 채로 다음 단계를 다른 터미널에서 진행합니다.

## 3. 더미 데이터 넣기

```bash
docker exec -i quespot-perf-mysql mysql -uquespot -pquespot_perf quespot_perf < perf/seed/01_seed_missions.sql
```

마지막에 `perf_spots = 100000`, `active_missions ≈ 95000`이 나오면 정상입니다. 다시 실행하면 이전 더미 데이터를 지우고 새로 만듭니다.

## 4. 실행 계획 측정

```bash
mkdir -p perf/results
docker exec -i quespot-perf-mysql mysql -uquespot -pquespot_perf quespot_perf \
  < perf/sql/explain_distance.sql > perf/results/before-explain.txt
```

`before-explain.txt`에서 볼 것:

- `EXPLAIN`의 `type` (`ALL` = 풀 스캔), `rows`, `Extra`(`Using filesort`)
- `EXPLAIN ANALYZE`의 `actual time`, 읽은 `rows`
- `SHOW PROFILES`의 쿼리 단독 실행 시간

## 5. 부하 테스트

JWT 필터가 Redis의 로그인 세션도 확인하므로, k6 가상 사용자 세션을 먼저 등록합니다. (Redis를 다시 띄우면 재실행)

```bash
bash perf/seed/02_seed_sessions.sh
```

```bash
brew install k6   # 처음 한 번

# 워밍업 (결과 버림)
k6 run -e JWT_SECRET=$JWT_SECRET -e DURATION=30s perf/k6/mission-distance.js

# 측정
k6 run -e JWT_SECRET=$JWT_SECRET -e RATE=20 -e DURATION=2m \
  --summary-export perf/results/before-k6-rate20.json perf/k6/mission-distance.js
```

- `JWT_SECRET`은 2단계에서 `source perf/perf.env`를 한 터미널이면 그대로 쓸 수 있습니다.
- 한 번의 시나리오 = 사용자 1명이 거리순 목록을 3페이지까지 넘겨 보는 흐름입니다.
- `RATE`를 10 → 20 → 40으로 올려 가며 응답 시간이 급격히 나빠지는 지점(포화점)도 기록합니다.

봐야 할 지표: `mission_list_first_page`, `mission_list_next_page`의 p50·p95·p99, `http_req_failed`, `http_reqs`(처리량)

## 6. 결과 기록

| 시나리오 | 지표 | Before | After |
| --- | --- | --- | --- |
| 첫 페이지 쿼리 | EXPLAIN type / rows | | |
| 첫 페이지 쿼리 | 단독 실행 시간 | | |
| API (RATE=20) | 첫 페이지 p95 | | |
| API (RATE=20) | 다음 페이지 p95 | | |
| API | 오류율 1% 미만 유지 최대 RATE | | |

## 정리

```bash
docker compose -f perf/docker-compose.perf.yml down -v   # 컨테이너와 테스트 데이터 삭제
```
