package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.repository.projection.MissionListProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MissionDiscoveryQueryRepository extends Repository<Mission, Long> {

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
