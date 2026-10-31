-- 거리순 미션 목록 쿼리 실행 계획 측정
-- MissionDiscoveryQueryRepository.findMissionListByDistance와 동일한 SQL (category·keyword 없음, size 20)

-- 기준 위치: 서울시청
SET @lat := 37.5665;
SET @lng := 126.9780;

SELECT '== 데이터 규모 ==' AS section;
SELECT COUNT(*) AS total_missions,
       SUM(status = 'ACTIVE') AS active_missions
  FROM missions;

SELECT '== missions 인덱스 ==' AS section;
SHOW INDEX FROM missions;

SELECT '== [1] 첫 페이지 (cursor 없음) EXPLAIN ==' AS section;
EXPLAIN FORMAT=TRADITIONAL
select ranked.mission_id
from (
    select m.id as mission_id,
           ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) as sort_value
    from missions m
    where m.status = 'ACTIVE'
      and (NULL is null or m.category = NULL)
) ranked
where NULL is null
   or ranked.sort_value > NULL
order by ranked.sort_value, ranked.mission_id
limit 21;

SELECT '== [1] 첫 페이지 (cursor 없음) EXPLAIN ANALYZE ==' AS section;
EXPLAIN ANALYZE
select
    ranked.mission_id as missionId,
    ranked.title as title,
    ranked.category as category,
    ranked.spot_name as spotName,
    ranked.address as address,
    ranked.image_url as imageUrl,
    ranked.reward_point as rewardPoint,
    ranked.estimated_minutes as estimatedMinutes,
    ranked.sort_value as sortValue
from (
    select
        m.id as mission_id,
        m.title,
        m.category,
        m.snapshot_name as spot_name,
        m.snapshot_address as address,
        m.snapshot_image_url as image_url,
        m.reward_point,
        m.estimated_minutes,
        ST_Distance_Sphere(
            POINT(m.snapshot_longitude, m.snapshot_latitude),
            POINT(@lng, @lat)
        ) as sort_value
    from missions m
    where m.status = 'ACTIVE'
      and (NULL is null or m.category = NULL)
      and (
          NULL is null
          or lower(m.title) like concat('%', NULL, '%') escape '!'
          or lower(m.snapshot_name) like concat('%', NULL, '%') escape '!'
      )
) ranked
where NULL is null
   or ranked.sort_value > NULL
   or (ranked.sort_value = NULL and ranked.mission_id > NULL)
order by ranked.sort_value, ranked.mission_id
limit 21;

-- 깊은 페이지: 기준 위치에서 30km 이후 구간
SET @cursor_value := 30000;
SET @cursor_id := 0;

SELECT '== [2] 깊은 페이지 (cursor = 30km) EXPLAIN ANALYZE ==' AS section;
EXPLAIN ANALYZE
select
    ranked.mission_id as missionId,
    ranked.sort_value as sortValue
from (
    select
        m.id as mission_id,
        m.title,
        m.category,
        m.snapshot_name as spot_name,
        m.snapshot_address as address,
        m.snapshot_image_url as image_url,
        m.reward_point,
        m.estimated_minutes,
        ST_Distance_Sphere(
            POINT(m.snapshot_longitude, m.snapshot_latitude),
            POINT(@lng, @lat)
        ) as sort_value
    from missions m
    where m.status = 'ACTIVE'
      and (NULL is null or m.category = NULL)
) ranked
where @cursor_value is null
   or ranked.sort_value > @cursor_value
   or (ranked.sort_value = @cursor_value and ranked.mission_id > @cursor_id)
order by ranked.sort_value, ranked.mission_id
limit 21;

-- 쿼리 단독 실행 시간 5회 측정
SELECT '== [3] 첫 페이지 단독 실행 5회 (profiling) ==' AS section;
SET profiling = 1;
SET profiling_history_size = 10;
SELECT m.id, ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) d
  FROM missions m WHERE m.status = 'ACTIVE' ORDER BY d, m.id LIMIT 21;
SELECT m.id, ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) d
  FROM missions m WHERE m.status = 'ACTIVE' ORDER BY d, m.id LIMIT 21;
SELECT m.id, ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) d
  FROM missions m WHERE m.status = 'ACTIVE' ORDER BY d, m.id LIMIT 21;
SELECT m.id, ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) d
  FROM missions m WHERE m.status = 'ACTIVE' ORDER BY d, m.id LIMIT 21;
SELECT m.id, ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) d
  FROM missions m WHERE m.status = 'ACTIVE' ORDER BY d, m.id LIMIT 21;
SHOW PROFILES;
SET profiling = 0;
