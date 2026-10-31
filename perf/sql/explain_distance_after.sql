-- 1차 개선 후 거리순 미션 목록 쿼리 실행 계획 측정
-- MissionDiscoveryQueryRepository.findMissionListByDistanceWithinRadius와 동일한 SQL (category·keyword 없음, size 20)

-- 기준 위치: 서울시청
SET @lat := 37.5665;
SET @lng := 126.9780;
-- MissionSearchRadius와 동일한 bounding box 계산 (지구 반지름 6,370,986m, 여유 1%)
SET @meters_per_degree := PI() * 6370986 / 180;

SELECT '== missions 인덱스 ==' AS section;
SHOW INDEX FROM missions WHERE Key_name = 'ix_missions_status_lat_lng';

-- 첫 페이지: 5km 반경
SET @radius := 5000;
SET @lat_delta := @radius * 1.01 / @meters_per_degree;
SET @lng_delta := @lat_delta / COS(RADIANS(@lat));

SELECT '== [1] 첫 페이지 (5km 반경) EXPLAIN ==' AS section;
EXPLAIN FORMAT=TRADITIONAL
select m.id
from missions m
where m.status = 'ACTIVE'
  and m.snapshot_latitude between @lat - @lat_delta and @lat + @lat_delta
  and m.snapshot_longitude between @lng - @lng_delta and @lng + @lng_delta;

SELECT '== [1] 첫 페이지 (5km 반경) EXPLAIN ANALYZE ==' AS section;
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
      and m.snapshot_latitude between @lat - @lat_delta and @lat + @lat_delta
      and m.snapshot_longitude between @lng - @lng_delta and @lng + @lng_delta
      and (NULL is null or m.category = NULL)
      and (
          NULL is null
          or lower(m.title) like concat('%', NULL, '%') escape '!'
          or lower(m.snapshot_name) like concat('%', NULL, '%') escape '!'
      )
) ranked
where ranked.sort_value <= @radius
  and (
      NULL is null
      or ranked.sort_value > NULL
      or (ranked.sort_value = NULL and ranked.mission_id > NULL)
  )
order by ranked.sort_value, ranked.mission_id
limit 21;

-- 깊은 페이지: 기준 위치에서 30km 이후 구간 (50km 반경 단계)
SET @radius := 50000;
SET @lat_delta := @radius * 1.01 / @meters_per_degree;
SET @lng_delta := @lat_delta / COS(RADIANS(@lat));
SET @cursor_value := 30000;
SET @cursor_id := 0;

SELECT '== [2] 깊은 페이지 (cursor = 30km, 50km 반경) EXPLAIN ANALYZE ==' AS section;
EXPLAIN ANALYZE
select
    ranked.mission_id as missionId,
    ranked.sort_value as sortValue
from (
    select
        m.id as mission_id,
        ST_Distance_Sphere(
            POINT(m.snapshot_longitude, m.snapshot_latitude),
            POINT(@lng, @lat)
        ) as sort_value
    from missions m
    where m.status = 'ACTIVE'
      and m.snapshot_latitude between @lat - @lat_delta and @lat + @lat_delta
      and m.snapshot_longitude between @lng - @lng_delta and @lng + @lng_delta
) ranked
where ranked.sort_value <= @radius
  and (
      ranked.sort_value > @cursor_value
      or (ranked.sort_value = @cursor_value and ranked.mission_id > @cursor_id)
  )
order by ranked.sort_value, ranked.mission_id
limit 21;

-- 쿼리 단독 실행 시간 5회 측정 (5km 반경 첫 페이지)
SET @radius := 5000;
SET @lat_delta := @radius * 1.01 / @meters_per_degree;
SET @lng_delta := @lat_delta / COS(RADIANS(@lat));

SELECT '== [3] 첫 페이지 단독 실행 5회 (profiling) ==' AS section;
SET profiling = 1;
SET profiling_history_size = 10;
SELECT COUNT(*) FROM (SELECT m.id, ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) d
  FROM missions m WHERE m.status = 'ACTIVE'
   AND m.snapshot_latitude BETWEEN @lat - @lat_delta AND @lat + @lat_delta
   AND m.snapshot_longitude BETWEEN @lng - @lng_delta AND @lng + @lng_delta
  HAVING d <= @radius ORDER BY d, m.id LIMIT 21) t;
SELECT COUNT(*) FROM (SELECT m.id, ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) d
  FROM missions m WHERE m.status = 'ACTIVE'
   AND m.snapshot_latitude BETWEEN @lat - @lat_delta AND @lat + @lat_delta
   AND m.snapshot_longitude BETWEEN @lng - @lng_delta AND @lng + @lng_delta
  HAVING d <= @radius ORDER BY d, m.id LIMIT 21) t;
SELECT COUNT(*) FROM (SELECT m.id, ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) d
  FROM missions m WHERE m.status = 'ACTIVE'
   AND m.snapshot_latitude BETWEEN @lat - @lat_delta AND @lat + @lat_delta
   AND m.snapshot_longitude BETWEEN @lng - @lng_delta AND @lng + @lng_delta
  HAVING d <= @radius ORDER BY d, m.id LIMIT 21) t;
SELECT COUNT(*) FROM (SELECT m.id, ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) d
  FROM missions m WHERE m.status = 'ACTIVE'
   AND m.snapshot_latitude BETWEEN @lat - @lat_delta AND @lat + @lat_delta
   AND m.snapshot_longitude BETWEEN @lng - @lng_delta AND @lng + @lng_delta
  HAVING d <= @radius ORDER BY d, m.id LIMIT 21) t;
SELECT COUNT(*) FROM (SELECT m.id, ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) d
  FROM missions m WHERE m.status = 'ACTIVE'
   AND m.snapshot_latitude BETWEEN @lat - @lat_delta AND @lat + @lat_delta
   AND m.snapshot_longitude BETWEEN @lng - @lng_delta AND @lng + @lng_delta
  HAVING d <= @radius ORDER BY d, m.id LIMIT 21) t;
SHOW PROFILES;
SET profiling = 0;
