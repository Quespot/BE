-- 2차 개선(공간 인덱스) 실행 계획 측정
-- 같은 반경 조회를 1차 방식(B-tree ix_missions_status_lat_lng, 위경도 BETWEEN)과
-- 2차 방식(공간 인덱스 sx_missions_location, MBRContains)으로 나란히 비교
-- ix_missions_status_lat_lng는 엔티티에서 제거됐지만 성능 테스트 DB에는 남아 있어 비교에 사용

SET @meters_per_degree := PI() * 6370986 / 180;

SELECT '== 인덱스 ==' AS section;
SHOW INDEX FROM missions WHERE Key_name IN ('ix_missions_status_lat_lng', 'sx_missions_location');

-- ---------------------------------------------------------------------
-- [1] 서울시청 5km (첫 페이지, 미션 밀집 지역)
SET @lat := 37.5665, @lng := 126.9780, @radius := 5000, @cursor_value := NULL, @cursor_id := NULL;
SET @lat_delta := @radius * 1.01 / @meters_per_degree;
SET @lng_delta := @lat_delta / COS(RADIANS(@lat));

SELECT '== [1] 서울 5km / 1차 B-tree ==' AS section;
EXPLAIN ANALYZE
select ranked.mission_id, ranked.sort_value from (
    select m.id as mission_id,
           ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) as sort_value
    from missions m force index (ix_missions_status_lat_lng)
    where m.status = 'ACTIVE'
      and m.snapshot_latitude between @lat - @lat_delta and @lat + @lat_delta
      and m.snapshot_longitude between @lng - @lng_delta and @lng + @lng_delta
) ranked
where ranked.sort_value <= @radius
order by ranked.sort_value, ranked.mission_id
limit 21;

SELECT '== [1] 서울 5km / 2차 공간 인덱스 ==' AS section;
EXPLAIN ANALYZE
select ranked.mission_id, ranked.sort_value from (
    select m.id as mission_id,
           ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) as sort_value
    from missions m
    where m.status = 'ACTIVE'
      and MBRContains(ST_MakeEnvelope(POINT(@lng - @lng_delta, @lat - @lat_delta), POINT(@lng + @lng_delta, @lat + @lat_delta)), m.snapshot_location)
) ranked
where ranked.sort_value <= @radius
order by ranked.sort_value, ranked.mission_id
limit 21;

-- ---------------------------------------------------------------------
-- [2] 서울시청 50km 단계, 30km 이후 페이지 (1차에서 옵티마이저가 전체 조회를 고른 구간)
SET @radius := 50000, @cursor_value := 30000, @cursor_id := 0;
SET @lat_delta := @radius * 1.01 / @meters_per_degree;
SET @lng_delta := @lat_delta / COS(RADIANS(@lat));

SELECT '== [2] 서울 50km (30km 이후) / 1차 옵티마이저 선택 (인덱스 힌트 없음) ==' AS section;
EXPLAIN ANALYZE
select ranked.mission_id, ranked.sort_value from (
    select m.id as mission_id,
           ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) as sort_value
    from missions m ignore index (sx_missions_location)
    where m.status = 'ACTIVE'
      and m.snapshot_latitude between @lat - @lat_delta and @lat + @lat_delta
      and m.snapshot_longitude between @lng - @lng_delta and @lng + @lng_delta
) ranked
where ranked.sort_value <= @radius
  and (ranked.sort_value > @cursor_value or (ranked.sort_value = @cursor_value and ranked.mission_id > @cursor_id))
order by ranked.sort_value, ranked.mission_id
limit 21;

SELECT '== [2] 서울 50km (30km 이후) / 2차 공간 인덱스 ==' AS section;
EXPLAIN ANALYZE
select ranked.mission_id, ranked.sort_value from (
    select m.id as mission_id,
           ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) as sort_value
    from missions m
    where m.status = 'ACTIVE'
      and MBRContains(ST_MakeEnvelope(POINT(@lng - @lng_delta, @lat - @lat_delta), POINT(@lng + @lng_delta, @lat + @lat_delta)), m.snapshot_location)
) ranked
where ranked.sort_value <= @radius
  and (ranked.sort_value > @cursor_value or (ranked.sort_value = @cursor_value and ranked.mission_id > @cursor_id))
order by ranked.sort_value, ranked.mission_id
limit 21;

-- ---------------------------------------------------------------------
-- [3] 미션이 드문 지역(경북 내륙) 20km 단계
SET @lat := 36.4500, @lng := 128.3000, @radius := 20000, @cursor_value := NULL, @cursor_id := NULL;
SET @lat_delta := @radius * 1.01 / @meters_per_degree;
SET @lng_delta := @lat_delta / COS(RADIANS(@lat));

SELECT '== [3] 드문 지역 20km / 1차 옵티마이저 선택 ==' AS section;
EXPLAIN ANALYZE
select ranked.mission_id, ranked.sort_value from (
    select m.id as mission_id,
           ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) as sort_value
    from missions m ignore index (sx_missions_location)
    where m.status = 'ACTIVE'
      and m.snapshot_latitude between @lat - @lat_delta and @lat + @lat_delta
      and m.snapshot_longitude between @lng - @lng_delta and @lng + @lng_delta
) ranked
where ranked.sort_value <= @radius
order by ranked.sort_value, ranked.mission_id
limit 21;

SELECT '== [3] 드문 지역 20km / 2차 공간 인덱스 ==' AS section;
EXPLAIN ANALYZE
select ranked.mission_id, ranked.sort_value from (
    select m.id as mission_id,
           ST_Distance_Sphere(POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(@lng, @lat)) as sort_value
    from missions m
    where m.status = 'ACTIVE'
      and MBRContains(ST_MakeEnvelope(POINT(@lng - @lng_delta, @lat - @lat_delta), POINT(@lng + @lng_delta, @lat + @lat_delta)), m.snapshot_location)
) ranked
where ranked.sort_value <= @radius
order by ranked.sort_value, ranked.mission_id
limit 21;
