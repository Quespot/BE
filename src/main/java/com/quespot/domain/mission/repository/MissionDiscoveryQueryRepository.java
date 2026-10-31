package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.repository.projection.MissionListProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MissionDiscoveryQueryRepository extends Repository<Mission, Long> {

    // 마지막 반경 단계(전국) 조회: 반경 제한 없이 ACTIVE 미션 전체를 거리순으로 정렬
    @Query(value = """
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
                        POINT(:longitude, :latitude)
                    ) as sort_value
                from missions m
                where m.status = 'ACTIVE'
                  and (:category is null or m.category = :category)
                  and (
                      :keyword is null
                      or lower(m.title) like concat('%', :keyword, '%') escape '!'
                      or lower(m.snapshot_name) like concat('%', :keyword, '%') escape '!'
                  )
            ) ranked
            where :cursorValue is null
               or ranked.sort_value > :cursorValue
               or (ranked.sort_value = :cursorValue and ranked.mission_id > :cursorId)
            order by ranked.sort_value, ranked.mission_id
            limit :limit
            """, nativeQuery = true)
    List<MissionListProjection> findMissionListByDistance(
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("cursorValue") Double cursorValue,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );

    // 반경 단계 조회: bounding box(ix_missions_status_lat_lng 범위 스캔)로 후보를 줄인 뒤 실제 거리로 다시 거른다
    @Query(value = """
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
                        POINT(:longitude, :latitude)
                    ) as sort_value
                from missions m
                where m.status = 'ACTIVE'
                  and m.snapshot_latitude between :minLatitude and :maxLatitude
                  and m.snapshot_longitude between :minLongitude and :maxLongitude
                  and (:category is null or m.category = :category)
                  and (
                      :keyword is null
                      or lower(m.title) like concat('%', :keyword, '%') escape '!'
                      or lower(m.snapshot_name) like concat('%', :keyword, '%') escape '!'
                  )
            ) ranked
            where ranked.sort_value <= :radiusMeters
              and (
                  :cursorValue is null
                  or ranked.sort_value > :cursorValue
                  or (ranked.sort_value = :cursorValue and ranked.mission_id > :cursorId)
              )
            order by ranked.sort_value, ranked.mission_id
            limit :limit
            """, nativeQuery = true)
    List<MissionListProjection> findMissionListByDistanceWithinRadius(
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("minLatitude") double minLatitude,
            @Param("maxLatitude") double maxLatitude,
            @Param("minLongitude") double minLongitude,
            @Param("maxLongitude") double maxLongitude,
            @Param("radiusMeters") int radiusMeters,
            @Param("cursorValue") Double cursorValue,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );

    @Query(value = """
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
                    CRC32(CONCAT(CAST(m.id AS CHAR), ':', CAST(:seed AS CHAR))) as sort_value
                from missions m
                where m.status = 'ACTIVE'
                  and (:category is null or m.category = :category)
                  and (
                      :keyword is null
                      or lower(m.title) like concat('%', :keyword, '%') escape '!'
                      or lower(m.snapshot_name) like concat('%', :keyword, '%') escape '!'
                  )
            ) ranked
            where :cursorValue is null
               or ranked.sort_value > :cursorValue
               or (ranked.sort_value = :cursorValue and ranked.mission_id > :cursorId)
            order by ranked.sort_value, ranked.mission_id
            limit :limit
            """, nativeQuery = true)
    List<MissionListProjection> findMissionListRandomly(
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("seed") long seed,
            @Param("cursorValue") Double cursorValue,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );
}
