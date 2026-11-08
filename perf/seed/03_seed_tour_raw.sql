-- TourAPI 파이프라인(정제·미션 후보 생성) 성능 테스트용 원본 데이터
-- 앱 실행으로 스키마가 생성된 뒤 실행
-- 더미 데이터 식별: tour_contents_raw.content_id / spots.source_content_id 'TP%'
-- 콘텐츠마다 수정 이력 2건(이전 버전·최신 버전)을 넣어, 정제가 content_id별 최신 행을 고르는 상황을 재현

-- docker exec mysql 클라이언트 기본 문자셋이 utf8mb4가 아니면 한글이 깨지고 길이 초과 오류가 난다
SET NAMES utf8mb4;

-- 콘텐츠 수 (서울 현재 규모 8,000 / 전국 확장 가정 40,000)
SET @content_count := IFNULL(@content_count, 8000);
SET SESSION cte_max_recursion_depth = 1000000;

-- 이전 더미 데이터 정리 (재실행 가능)
DELETE m FROM missions m
  JOIN spots s ON s.id = m.spot_id
 WHERE s.source_content_id LIKE 'TP%';

DELETE c FROM mission_candidates c
  JOIN spots s ON s.id = c.spot_id
 WHERE s.source_content_id LIKE 'TP%';

DELETE FROM spots WHERE source_content_id LIKE 'TP%';
DELETE FROM tour_contents_raw WHERE content_id LIKE 'TP%';

-- 서울 25개 자치구 (주소 문자열용)
DROP TEMPORARY TABLE IF EXISTS perf_gu;
CREATE TEMPORARY TABLE perf_gu (
    id   INT PRIMARY KEY,
    name VARCHAR(10) NOT NULL
);
INSERT INTO perf_gu VALUES
    (0, '종로구'), (1, '중구'), (2, '용산구'), (3, '성동구'), (4, '광진구'),
    (5, '동대문구'), (6, '중랑구'), (7, '성북구'), (8, '강북구'), (9, '도봉구'),
    (10, '노원구'), (11, '은평구'), (12, '서대문구'), (13, '마포구'), (14, '양천구'),
    (15, '강서구'), (16, '구로구'), (17, '금천구'), (18, '영등포구'), (19, '동작구'),
    (20, '관악구'), (21, '서초구'), (22, '강남구'), (23, '송파구'), (24, '강동구');

-- 분류 코드 (CategoryMappingSeeder 매핑 대상과 미매핑 코드를 섞음)
DROP TEMPORARY TABLE IF EXISTS perf_lcls;
CREATE TEMPORARY TABLE perf_lcls (
    id      INT PRIMARY KEY,
    systm1  VARCHAR(3) NOT NULL,
    systm2  VARCHAR(5) NOT NULL,
    systm3  VARCHAR(9) NOT NULL,
    type_id INT NOT NULL
);
INSERT INTO perf_lcls VALUES
    (0, 'HS', 'HS01', 'HS010100', 12),
    (1, 'HS', 'HS02', 'HS020100', 12),
    (2, 'NA', 'NA01', 'NA010100', 12),
    (3, 'NA', 'NA04', 'NA040200', 12),
    (4, 'VE', 'VE01', 'VE010200', 12),
    (5, 'VE', 'VE04', 'VE040300', 14),
    (6, 'EX', 'EX01', 'EX010100', 14),
    (7, 'EX', 'EX06', 'EX060100', 14),
    (8, 'LS', 'LS01', 'LS010100', 28),
    (9, 'SH', 'SH06', 'SH060100', 38),
    (10, 'FD', 'FD01', 'FD010100', 39),
    (11, 'FD', 'FD02', 'FD020100', 39),
    (12, 'FD', 'FD04', 'FD040300', 39),
    (13, 'FD', 'FD05', 'FD050100', 39);

DROP TEMPORARY TABLE IF EXISTS perf_content;
CREATE TEMPORARY TABLE perf_content (
    n      INT PRIMARY KEY,
    lat    DECIMAL(13, 10) NOT NULL,
    lng    DECIMAL(13, 10) NOT NULL,
    gu_id  INT NOT NULL,
    lcls   INT NOT NULL,
    image  BOOLEAN NOT NULL
);
INSERT INTO perf_content
WITH RECURSIVE seq (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @content_count
)
SELECT
    n,
    -- 서울 경계 사각형 안의 임의 위치 (일부는 경계 밖이라 주소 기반 판정으로 넘어감)
    ROUND(37.43 + RAND() * 0.27, 10),
    ROUND(126.76 + RAND() * 0.43, 10),
    FLOOR(RAND() * 25),
    FLOOR(RAND() * 14),
    RAND() < 0.85
FROM seq;

-- 이전 버전(2026-01) + 최신 버전(2026-06) 원본 2건씩
INSERT INTO tour_contents_raw (
    content_id, operation, payload, api_modified_time, show_flag, fetched_at
)
SELECT
    CONCAT('TP', LPAD(c.n, 7, '0')),
    'areaBasedSyncList2',
    JSON_OBJECT(
        'contentid', CONCAT('TP', LPAD(c.n, 7, '0')),
        'contenttypeid', CAST(l.type_id AS CHAR),
        'title', CONCAT('파이프라인 테스트 장소 ', c.n, IF(v.version = 1, '', ' (갱신)')),
        'addr1', CONCAT('서울특별시 ', g.name, ' 테스트로 ', c.n),
        'addr2', '',
        'zipcode', '04524',
        'tel', '',
        'mapx', CAST(c.lng AS CHAR),
        'mapy', CAST(c.lat AS CHAR),
        'mlevel', '6',
        'firstimage', IF(c.image, CONCAT('https://tong.visitkorea.or.kr/cms/resource/perf/', c.n, '_image2_1.jpg'), ''),
        'firstimage2', IF(c.image, CONCAT('https://tong.visitkorea.or.kr/cms/resource/perf/', c.n, '_image3_1.jpg'), ''),
        'createdtime', '20250101000000',
        'modifiedtime', DATE_FORMAT(v.modified_at + INTERVAL (c.n % 86400) SECOND, '%Y%m%d%H%i%s'),
        'showflag', '1',
        'cpyrhtDivCd', 'Type3',
        'lDongRegnCd', '11',
        'lDongSignguCd', LPAD(110 + c.gu_id * 10, 3, '0'),
        'lclsSystm1', l.systm1,
        'lclsSystm2', l.systm2,
        'lclsSystm3', l.systm3
    ),
    v.modified_at + INTERVAL (c.n % 86400) SECOND,
    TRUE,
    NOW(6)
FROM perf_content c
JOIN perf_gu g ON g.id = c.gu_id
JOIN perf_lcls l ON l.id = c.lcls
CROSS JOIN (
    SELECT 1 AS version, TIMESTAMP('2026-01-01 00:00:00') AS modified_at
    UNION ALL
    SELECT 2, TIMESTAMP('2026-06-01 00:00:00')
) v;

DROP TEMPORARY TABLE IF EXISTS perf_content;

SELECT
    COUNT(DISTINCT content_id) AS perf_contents,
    COUNT(*) AS perf_raw_rows
FROM tour_contents_raw
WHERE content_id LIKE 'TP%';
