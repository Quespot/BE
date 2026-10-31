-- =============================================================
-- 분류체계 → 앱 카테고리 매핑
--
-- 매칭 규칙: spots.lcls_systm3 → lcls_systm2 → lcls_systm1 순으로
--            category_mappings 를 조회해 가장 먼저 걸리는 것을 쓴다.
--            (= priority 가 높은 것이 이긴다)
--
-- app_category 값
--   HISTORY / CULTURE / NATURE / FOOD / NIGHT_VIEW : 미션 후보 대상
--   EXCLUDED : 검토했고 미션에 쓰지 않기로 한 분류
--   (매핑 없음) : spots 에는 UNMAPPED 로 저장하되 후보 생성에서 제외.
--                 나중에 이것만 훑어 누락을 검토한다.
--
-- priority : 대분류 10 / 중분류 20 / 소분류 30
-- =============================================================

-- -------------------------------------------------------------
-- 대분류 (priority 10)
-- -------------------------------------------------------------
INSERT INTO category_mappings (lcls_code, content_type_id, app_category, priority, version, created_at) VALUES
    ('HS',  NULL, 'HISTORY',  10, 1, NOW()),   -- 역사관광
    ('NA',  NULL, 'NATURE',   10, 1, NOW()),   -- 자연관광
    ('FD',  NULL, 'FOOD',     10, 1, NOW()),   -- 음식
    ('VE',  NULL, 'CULTURE',  10, 1, NOW()),   -- 문화관광
    ('AC',  NULL, 'EXCLUDED', 10, 1, NOW()),   -- 숙박: 미션 대상 아님
    ('EV',  NULL, 'EXCLUDED', 10, 1, NOW()),   -- 축제/공연/행사: 기간 만료 처리 필요
    ('C01', NULL, 'EXCLUDED', 10, 1, NOW()),   -- 추천코스: 장소가 아님
    ('LS',  NULL, 'EXCLUDED', 10, 1, NOW());   -- 레저스포츠: 시설 중심, 사진 인증 부적합

-- -------------------------------------------------------------
-- 중분류 예외 (priority 20)
-- -------------------------------------------------------------
INSERT INTO category_mappings (lcls_code, content_type_id, app_category, priority, version, created_at) VALUES
    -- VE 안에 자연물이 섞여 있다
    ('VE03', NULL, 'NATURE',   20, 1, NOW()),  -- 도시공원 (시민/근린/어린이공원)

    -- VE 안에 미션으로 쓸 수 없는 시설이 섞여 있다
    ('VE05', NULL, 'EXCLUDED', 20, 1, NOW()),  -- 복합관광시설 (관광단지/리조트)
    ('VE08', NULL, 'EXCLUDED', 20, 1, NOW()),  -- 행사시설 (연회장)
    ('VE10', NULL, 'EXCLUDED', 20, 1, NOW()),  -- 레저스포츠시설 (경기장/스포츠센터)
    ('VE11', NULL, 'EXCLUDED', 20, 1, NOW()),  -- 교통시설 (공항/지하철/터미널)

    -- SH 는 대분류 매핑이 없다. 시장만 살린다
    ('SH06', NULL, 'CULTURE',  20, 1, NOW());  -- 시장 (광장시장, 남대문시장 등)

-- -------------------------------------------------------------
-- 소분류 예외 (priority 30)
-- -------------------------------------------------------------
INSERT INTO category_mappings (lcls_code, content_type_id, app_category, priority, version, created_at) VALUES
    -- 야경 미션의 유일한 소스
    ('VE010200', NULL, 'NIGHT_VIEW', 30, 1, NOW()),  -- 타워 / 전망대

    -- VE 안의 자연물
    ('VE040300', NULL, 'NATURE',     30, 1, NOW()),  -- 둘레길

    -- VE09(교육시설)는 CULTURE 지만 학교·어학당은 관광지가 아니다
    ('VE090500', NULL, 'EXCLUDED',   30, 1, NOW()),  -- 어학당
    ('VE090600', NULL, 'EXCLUDED',   30, 1, NOW()),  -- 학교

    -- 기타 부적합
    ('VE070400', NULL, 'EXCLUDED',   30, 1, NOW()),  -- 컨벤션센터
    ('VE120200', NULL, 'EXCLUDED',   30, 1, NOW()),  -- 카지노
    ('FD040300', NULL, 'EXCLUDED',   30, 1, NOW()),  -- 클럽: 연령 이슈

    -- EX 는 대분류 매핑이 없다. 관광지 성격인 것만 살린다
    ('EX010100', NULL, 'CULTURE',    30, 1, NOW()),  -- 전통문화체험
    ('EX060100', NULL, 'CULTURE',    30, 1, NOW());  -- 근대산업유산
