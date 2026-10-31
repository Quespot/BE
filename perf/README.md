# Quespot 성능 테스트

운영 서버와 분리된 로컬 환경에서 같은 조건으로 **개선 전(Before)과 개선 후(After)**를 측정하기 위한 스크립트입니다.

```text
perf/
├── docker-compose.perf.yml   # 성능 테스트 전용 MySQL(3308)·Redis(6380)
├── perf.env                  # 로컬 더미 환경변수 (운영 값 아님)
├── seed/01_seed_missions.sql # 미션 10만 건 더미 데이터
├── seed/02_seed_sessions.sh  # k6 가상 사용자 로그인 세션(Redis) 등록
├── sql/explain_distance.sql  # 거리순 목록 쿼리 실행 계획 (개선 전)
├── sql/explain_distance_after.sql # 거리순 목록 쿼리 실행 계획 (1차 개선 후, 반경 단계 조회)
├── sql/explain_distance_after2.sql # 1차(B-tree) vs 2차(공간 인덱스) 실행 계획 비교
├── k6/mission-distance.js    # 거리순 목록 API 부하 테스트 (AREA=city|rural)
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

# 1차 개선 후
docker exec -i quespot-perf-mysql mysql -uquespot -pquespot_perf quespot_perf \
  < perf/sql/explain_distance_after.sql > perf/results/after1-explain.txt

# 2차 개선 후 (첫 실행은 캐시 워밍용으로 버리고 두 번째 결과를 저장)
docker exec -i quespot-perf-mysql mysql -uquespot -pquespot_perf quespot_perf \
  < perf/sql/explain_distance_after2.sql > /dev/null
docker exec -i quespot-perf-mysql mysql -uquespot -pquespot_perf quespot_perf \
  < perf/sql/explain_distance_after2.sql > perf/results/after2-explain.txt
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

# k6를 실행하는 터미널에서 JWT_SECRET 등 환경변수 불러오기
set -a; source perf/perf.env; set +a

# 워밍업 (결과 버림)
k6 run -e JWT_SECRET=$JWT_SECRET -e DURATION=30s perf/k6/mission-distance.js

# 측정 (RATE 2·5는 1분, 20은 2분 / 결과 파일 이름은 before·after1 등 단계별로 구분)
k6 run -e JWT_SECRET=$JWT_SECRET -e RATE=2 -e DURATION=1m \
  --summary-export perf/results/before-k6-rate2.json perf/k6/mission-distance.js
k6 run -e JWT_SECRET=$JWT_SECRET -e RATE=5 -e DURATION=1m \
  --summary-export perf/results/before-k6-rate5.json perf/k6/mission-distance.js
k6 run -e JWT_SECRET=$JWT_SECRET -e RATE=20 -e DURATION=2m \
  --summary-export perf/results/before-k6-rate20.json perf/k6/mission-distance.js
```

- 한 번의 시나리오 = 사용자 1명이 거리순 목록을 3페이지까지 넘겨 보는 흐름입니다.
- `AREA=rural`을 주면 도시 주변이 아닌 전국 임의 위치 사용자로 측정합니다 (미션이 드물어 20km·50km 반경 단계를 자주 사용).
- `RATE`를 2 → 5 → 20으로 올려 가며 응답 시간이 급격히 나빠지는 지점(포화점)도 기록합니다.
- 목표 부하를 처리하지 못해 누락된 시나리오(`dropped_iterations`)가 생기면 k6가 실패로 표시합니다. 포화 구간 측정(개선 전 RATE=20 등)에서는 의도된 결과입니다.

봐야 할 지표: `mission_list_first_page`, `mission_list_next_page`의 p50·p95·p99, `http_req_failed`, `http_reqs`(처리량)

## 6. 결과 (미션 10만 건, ACTIVE 95,084건)

### API — 도시 주변 사용자 (AREA=city)

| RATE | 지표 | Before | 1차 개선 | 2차 개선 |
| --- | --- | --- | --- | --- |
| 2 | 첫 페이지 p95 | 433ms | 83ms | - |
| 5 | 첫 페이지 p95 | 2.41s | 64ms | - |
| 20 | 첫 페이지 p95 | 19.1s | 27ms | 20ms |
| 20 | 다음 페이지 p95 | 19.2s | 25ms | 19ms |
| 20 | 처리량 / 처리 못 한 시나리오 | 10.6 req/s / 1,841 | 60.0 req/s / 0 | 60.0 req/s / 0 |

### API — 미션이 드문 지역 포함 (AREA=rural)

| RATE | 지표 | 1차 개선 | 2차 개선 |
| --- | --- | --- | --- |
| 5 | 첫 페이지 p95 | 271ms | 38ms |
| 20 | 첫 페이지 p95 | 4.07s | 21ms |
| 20 | 다음 페이지 p95 | 4.07s | 20ms |
| 20 | 처리량 / 처리 못 한 시나리오 | 52.0 req/s / 189 | 60.0 req/s / 0 |

오류율은 모두 0%입니다.

### 쿼리

| 구간 | Before | 1차 개선 (B-tree) | 2차 개선 (공간 인덱스) |
| --- | --- | --- | --- |
| 서울 5km 첫 페이지 | 95,084행 + filesort, 206ms | 위도 범위 7,745행, 8.3ms | 2,109행, 9.9ms |
| 서울 50km 단계 (30km 이후 페이지) | 95,084행, 335ms | 옵티마이저가 전체 조회 선택, 95,084행, 184ms | 30,340행, 109ms |
| 드문 지역 20km 단계 | - | 위도 범위 5,544행, 4.8ms | 379행, 1.4ms |

- 1차(B-tree 복합 인덱스)는 위도 범위만 탐색 범위를 줄여, 반경이 커지면 위도 띠가 넓어져 옵티마이저가 전체 조회로 돌아갑니다. 드문 지역 사용자는 50km 단계까지 자주 내려가서 이 구간이 병목이 됐습니다.
- 2차(공간 인덱스, R-tree)는 위도·경도를 함께 좁혀 반경이 커져도 반경 안의 미션만 읽습니다.
- 미션이 밀집한 도심 5km에서는 두 방식이 비슷합니다 (B-tree는 커버링 인덱스로 읽는 반면 공간 인덱스는 행 조회가 필요).
- 50km 단계의 깊은 페이지는 반경 안의 미션(약 3만 건)을 모두 정렬하므로 여전히 100ms대입니다.

## 정리

```bash
docker compose -f perf/docker-compose.perf.yml down -v   # 컨테이너와 테스트 데이터 삭제
```
