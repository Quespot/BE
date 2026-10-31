-- 거리 기반 미션 탐색 성능 테스트용 더미 데이터 (spots·mission_candidates·missions 각 10만 건)
-- 앱 실행으로 스키마가 생성된 뒤 실행
-- 더미 데이터 식별: spots.source_content_id 'PERF-%', mission_candidates.generator_version 999

SET @row_count := 100000;
SET SESSION cte_max_recursion_depth = 1000000;

-- 이전 더미 데이터 정리 (재실행 가능)
DELETE m FROM missions m
  JOIN spots s ON s.id = m.spot_id
 WHERE s.source_content_id LIKE 'PERF-%';

DELETE c FROM mission_candidates c
  JOIN spots s ON s.id = c.spot_id
 WHERE s.source_content_id LIKE 'PERF-%';

DELETE FROM spots WHERE source_content_id LIKE 'PERF-%';

-- 좌표 분포 기준 도시 (70%는 도시 주변, 30%는 전국 균등 분포)
DROP TEMPORARY TABLE IF EXISTS perf_city;
CREATE TEMPORARY TABLE perf_city (
    id     INT PRIMARY KEY,
    name   VARCHAR(20) NOT NULL,
    lat    DOUBLE NOT NULL,
    lng    DOUBLE NOT NULL,
    spread DOUBLE NOT NULL
);
INSERT INTO perf_city VALUES
    (1,  '서울', 37.5665, 126.9780, 0.12),
    (2,  '부산', 35.1796, 129.0756, 0.10),
    (3,  '인천', 37.4563, 126.7052, 0.10),
    (4,  '대구', 35.8714, 128.6014, 0.08),
    (5,  '대전', 36.3504, 127.3845, 0.07),
    (6,  '광주', 35.1595, 126.8526, 0.07),
    (7,  '제주', 33.4996, 126.5312, 0.15),
    (8,  '강릉', 37.7519, 128.8761, 0.08),
    (9,  '경주', 35.8562, 129.2247, 0.07),
    (10, '전주', 35.8242, 127.1480, 0.06);

-- 행별 난수 고정 (RAND() 재계산으로 위도·경도 분포가 어긋나지 않도록 임시 테이블에 저장)
DROP TEMPORARY TABLE IF EXISTS perf_seed;
CREATE TEMPORARY TABLE perf_seed (
    n       INT PRIMARY KEY,
    r_mode  DOUBLE NOT NULL,
    city_id INT NOT NULL,
    dlat    DOUBLE NOT NULL,
    dlng    DOUBLE NOT NULL,
    ulat    DOUBLE NOT NULL,
    ulng    DOUBLE NOT NULL,
    cat     INT NOT NULL,
    r_stat  DOUBLE NOT NULL
);

INSERT INTO perf_seed (n, r_mode, city_id, dlat, dlng, ulat, ulng, cat, r_stat)
WITH RECURSIVE seq (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @row_count
)
SELECT
    n,
    RAND(),
    -- 서울 비중을 높인 도시 가중치
    ELT(1 + FLOOR(RAND() * 20), 1, 1, 1, 1, 1, 1, 2, 2, 2, 3, 3, 4, 4, 5, 6, 7, 7, 8, 9, 10),
    RAND() + RAND() + RAND() - 1.5,
    RAND() + RAND() + RAND() - 1.5,
    RAND(),
    RAND(),
    FLOOR(RAND() * 5),
    RAND()
FROM seq;

-- spots
INSERT INTO spots (
    source, source_content_id, name, address,
    latitude, longitude, app_category, show_flag,
    created_at, updated_at
)
SELECT
    'MANUAL',
    CONCAT('PERF-', LPAD(p.n, 6, '0')),
    CONCAT('성능테스트 장소 ', p.n),
    CONCAT(c.name, ' 테스트로 ', p.n),
    ROUND(IF(p.r_mode < 0.7, c.lat + p.dlat * c.spread * 2, 34.3 + p.ulat * 4.0), 10),
    ROUND(IF(p.r_mode < 0.7, c.lng + p.dlng * c.spread * 2, 126.2 + p.ulng * 3.2), 10),
    ELT(1 + p.cat, 'HISTORY', 'CULTURE', 'NATURE', 'FOOD', 'NIGHT_VIEW'),
    TRUE,
    NOW(6),
    NOW(6)
FROM perf_seed p
JOIN perf_city c ON c.id = p.city_id;

-- mission_candidates (발행 완료 상태)
INSERT INTO mission_candidates (
    spot_id, template_code, generator_version,
    generated_title, generated_description,
    suggested_category, suggested_reward_point, suggested_estimated_minutes,
    status, created_at, updated_at
)
SELECT
    s.id,
    CONCAT(s.app_category, '_LOCATION'),
    999,
    CONCAT(s.name, ' 미션'),
    CONCAT(s.name, '에 방문해 위치 인증을 완료해 주세요.'),
    s.app_category,
    100,
    30,
    'PUBLISHED',
    NOW(6),
    NOW(6)
FROM spots s
WHERE s.source_content_id LIKE 'PERF-%';

-- missions (status 필터 검증을 위해 약 5%는 INACTIVE)
INSERT INTO missions (
    spot_id, candidate_id, title, description, category,
    reward_point, estimated_minutes, status, source,
    snapshot_name, snapshot_address, snapshot_latitude, snapshot_longitude,
    snapshot_image_url, snapshot_district_code,
    created_at, updated_at
)
SELECT
    s.id,
    c.id,
    c.generated_title,
    c.generated_description,
    c.suggested_category,
    c.suggested_reward_point,
    c.suggested_estimated_minutes,
    IF(p.r_stat < 0.95, 'ACTIVE', 'INACTIVE'),
    'MANUAL',
    s.name,
    s.address,
    s.latitude,
    s.longitude,
    NULL,
    NULL,
    NOW(6),
    NOW(6)
FROM mission_candidates c
JOIN spots s ON s.id = c.spot_id
JOIN perf_seed p ON p.n = CAST(SUBSTRING(s.source_content_id, 6) AS UNSIGNED)
WHERE c.generator_version = 999;

DROP TEMPORARY TABLE IF EXISTS perf_seed;
DROP TEMPORARY TABLE IF EXISTS perf_city;

-- 통계 갱신 및 결과 확인
ANALYZE TABLE spots, mission_candidates, missions;

SELECT
    (SELECT COUNT(*) FROM spots WHERE source_content_id LIKE 'PERF-%') AS perf_spots,
    (SELECT COUNT(*) FROM missions) AS total_missions,
    (SELECT COUNT(*) FROM missions WHERE status = 'ACTIVE') AS active_missions;
