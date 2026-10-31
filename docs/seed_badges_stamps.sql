-- =============================================================
-- 배지 · 스탬프 마스터 시드 (참고용, #50)
--
-- 실제 시드는 RewardMasterDataSeeder가 기동 시 자동으로 한다(코드 기준 upsert,
-- 목록에 없는 활성 배지는 is_active=false). 이 파일은 수동 적용·검토용 사본이다.
--
-- condition_json: { "metric", "scope"?: { "regionCode" }, "threshold" }
--   MISSION_COMPLETED                    완료 미션 수 (scope.regionCode로 시도 한정)
--   MISSION_COMPLETED_DISTINCT_DISTRICT  서로 다른 시군구 수 (scope.regionCode 필수)
--   PHOTO_REGISTERED                     mission_photos + archive_photos 등록 수
--   COURSE_COMPLETED                     완주 코스 수
-- 부산 탐험(BUSAN_EXPLORER)은 조건 미정으로 보류, 웨스트 왕(WEST_KING)은 폐기.
-- =============================================================

INSERT INTO badges (code, name, description, icon_url, condition_json, sort_order, is_active) VALUES
    ('FIRST_MISSION', '첫 미션',     '첫 미션을 완료했어요',                       NULL, '{"metric":"MISSION_COMPLETED","threshold":1}', 1, TRUE),
    ('EXPLORER',      '탐험가',      '서울의 서로 다른 구에서 미션 5개를 완료했어요', NULL, '{"metric":"MISSION_COMPLETED_DISTINCT_DISTRICT","scope":{"regionCode":"11"},"threshold":5}', 2, TRUE),
    ('PHOTOGRAPHER',  '사진작가',    '아카이브에 사진 10장을 등록했어요',           NULL, '{"metric":"PHOTO_REGISTERED","threshold":10}', 3, TRUE),
    ('SEOUL_MASTER',  '서울 마스터', '서울 미션 10개를 완료했어요',                 NULL, '{"metric":"MISSION_COMPLETED","scope":{"regionCode":"11"},"threshold":10}', 4, TRUE),
    ('QUEST_KING',    '퀘스트 왕',   '코스 5개를 완주했어요',                       NULL, '{"metric":"COURSE_COMPLETED","threshold":5}', 5, TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), description = VALUES(description),
    condition_json = VALUES(condition_json), sort_order = VALUES(sort_order), is_active = TRUE;

UPDATE badges SET is_active = FALSE WHERE code IN ('BUSAN_EXPLORER', 'WEST_KING');

-- 스탬프 8개 시도. 수집 대상이 서울뿐이라 서울만 활성(잠금 노출용 플래그).
-- 판정은 8개 전부 동작한다 — region_code(시도 코드)와 spots.ldong_regn_cd가 같으면 획득.
INSERT INTO stamps (code, name, region_code, icon_url, sort_order, is_active) VALUES
    ('SEOUL',     '서울', '11', NULL, 1, TRUE),
    ('BUSAN',     '부산', '26', NULL, 2, FALSE),
    ('JEJU',      '제주', '50', NULL, 3, FALSE),
    ('GYEONGJU',  '경주', '47', NULL, 4, FALSE),
    ('YEOSU',     '여수', '46', NULL, 5, FALSE),
    ('GANGNEUNG', '강릉', '42', NULL, 6, FALSE),
    ('JEONJU',    '전주', '45', NULL, 7, FALSE),
    ('INCHEON',   '인천', '28', NULL, 8, FALSE)
ON DUPLICATE KEY UPDATE id = id;
