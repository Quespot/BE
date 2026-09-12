package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.enums.MissionStatus;
import com.quespot.domain.mission.repository.projection.MissionListProjection;
import com.quespot.domain.mission.repository.projection.MissionSpotSummaryProjection;
import com.quespot.domain.mission.repository.projection.NearbyMissionSpotProjection;
import com.quespot.domain.mission.repository.projection.RecommendedMissionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    @Query("""
            select m.candidate.id
            from Mission m
            where m.candidate.id in :candidateIds
            """)
    List<Long> findPublishedCandidateIds(@Param("candidateIds") List<Long> candidateIds);

    Optional<Mission> findByIdAndStatus(Long missionId, MissionStatus status);

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

    @Query(value = """
            with base as (
                select
                    m.id as mission_id,
                    m.title,
                    m.category,
                    m.snapshot_name as spot_name,
                    m.snapshot_address as address,
                    m.snapshot_image_url as image_url,
                    m.reward_point,
                    m.estimated_minutes,
                    case
                        when :preferredCategories = ''
                          or find_in_set(m.category, :preferredCategories) > 0 then 0
                        else 1
                    end as preference_rank,
                    CRC32(CONCAT(m.category, ':', CAST(:seed AS CHAR))) as category_order,
                    case
                        when :latitude is null
                            then CRC32(CONCAT(CAST(m.id AS CHAR), ':', CAST(:seed AS CHAR)))
                        else ST_Distance_Sphere(
                            POINT(m.snapshot_longitude, m.snapshot_latitude),
                            POINT(:longitude, :latitude)
                        )
                    end as sort_value
                from missions m
                where m.status = 'ACTIVE'
                  and not exists (
                      select 1
                      from mission_attempts ma
                      where ma.user_id = :userId
                        and ma.mission_id = m.id
                        and ma.status in ('COMPLETED', 'IN_PROGRESS')
                  )
            ), ranked as (
                select
                    base.*,
                    row_number() over (
                        partition by base.category
                        order by base.sort_value, base.mission_id
                    ) as category_rank
                from base
            )
            select
                ranked.mission_id as missionId,
                ranked.title as title,
                ranked.category as category,
                ranked.spot_name as spotName,
                ranked.address as address,
                ranked.image_url as imageUrl,
                ranked.reward_point as rewardPoint,
                ranked.estimated_minutes as estimatedMinutes,
                ranked.sort_value as sortValue,
                ranked.preference_rank as preferenceRank,
                ranked.category_rank as categoryRank,
                ranked.category_order as categoryOrder
            from ranked
            where :cursorPreferenceRank is null
               or ranked.preference_rank > :cursorPreferenceRank
               or (
                   ranked.preference_rank = :cursorPreferenceRank
                   and ranked.category_rank > :cursorCategoryRank
               )
               or (
                   ranked.preference_rank = :cursorPreferenceRank
                   and ranked.category_rank = :cursorCategoryRank
                   and ranked.category_order > :cursorCategoryOrder
               )
               or (
                   ranked.preference_rank = :cursorPreferenceRank
                   and ranked.category_rank = :cursorCategoryRank
                   and ranked.category_order = :cursorCategoryOrder
                   and ranked.sort_value > :cursorSortValue
               )
               or (
                   ranked.preference_rank = :cursorPreferenceRank
                   and ranked.category_rank = :cursorCategoryRank
                   and ranked.category_order = :cursorCategoryOrder
                   and ranked.sort_value = :cursorSortValue
                   and ranked.mission_id > :cursorMissionId
               )
            order by
                ranked.preference_rank,
                ranked.category_rank,
                ranked.category_order,
                ranked.sort_value,
                ranked.mission_id
            limit :limit
            """, nativeQuery = true)
    List<RecommendedMissionProjection> findRecommendedMissions(
            @Param("userId") Long userId,
            @Param("preferredCategories") String preferredCategories,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("seed") long seed,
            @Param("cursorPreferenceRank") Integer cursorPreferenceRank,
            @Param("cursorCategoryRank") Long cursorCategoryRank,
            @Param("cursorCategoryOrder") Long cursorCategoryOrder,
            @Param("cursorSortValue") Double cursorSortValue,
            @Param("cursorMissionId") Long cursorMissionId,
            @Param("limit") int limit
    );

    @Query(value = """
            select
                m.snapshot_district_code as districtCode,
                count(distinct m.id) as missionCount,
                count(distinct case when ma.status = 'COMPLETED' then m.id end) as completedMissionCount
            from missions m
            left join mission_attempts ma
              on ma.mission_id = m.id
             and ma.user_id = :userId
             and ma.status = 'COMPLETED'
            where m.status = 'ACTIVE'
              and m.snapshot_district_code is not null
              and left(m.snapshot_district_code, 2) = :regionCode
            group by m.snapshot_district_code
            order by m.snapshot_district_code
            """, nativeQuery = true)
    List<MissionSpotSummaryProjection> findMissionSpotSummaries(
            @Param("userId") Long userId,
            @Param("regionCode") String regionCode
    );

    @Query(value = """
            select
                m.snapshot_district_code as districtCode,
                count(distinct m.id) as missionCount,
                count(distinct case when ma.status = 'COMPLETED' then m.id end) as completedMissionCount,
                min(ST_Distance_Sphere(
                    POINT(m.snapshot_longitude, m.snapshot_latitude),
                    POINT(:longitude, :latitude)
                )) as distanceMeters
            from missions m
            left join mission_attempts ma
              on ma.mission_id = m.id
             and ma.user_id = :userId
             and ma.status = 'COMPLETED'
            where m.status = 'ACTIVE'
              and m.snapshot_district_code is not null
            group by m.snapshot_district_code
            order by distanceMeters, m.snapshot_district_code
            limit :limit
            """, nativeQuery = true)
    List<NearbyMissionSpotProjection> findNearbyMissionSpots(
            @Param("userId") Long userId,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
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
                    ST_Distance_Sphere(
                        POINT(m.snapshot_longitude, m.snapshot_latitude),
                        POINT(:longitude, :latitude)
                    ) as sort_value
                from missions m
                where m.status = 'ACTIVE'
                  and m.snapshot_district_code = :districtCode
            ) ranked
            where :cursorValue is null
               or ranked.sort_value > :cursorValue
               or (ranked.sort_value = :cursorValue and ranked.mission_id > :cursorId)
            order by ranked.sort_value, ranked.mission_id
            limit :limit
            """, nativeQuery = true)
    List<MissionListProjection> findDistrictMissionsByDistance(
            @Param("districtCode") String districtCode,
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
                  and m.snapshot_district_code = :districtCode
            ) ranked
            where :cursorValue is null
               or ranked.sort_value > :cursorValue
               or (ranked.sort_value = :cursorValue and ranked.mission_id > :cursorId)
            order by ranked.sort_value, ranked.mission_id
            limit :limit
            """, nativeQuery = true)
    List<MissionListProjection> findDistrictMissionsRandomly(
            @Param("districtCode") String districtCode,
            @Param("seed") long seed,
            @Param("cursorValue") Double cursorValue,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );

}
