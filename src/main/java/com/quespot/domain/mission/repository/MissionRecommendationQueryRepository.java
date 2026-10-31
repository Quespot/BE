package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.repository.projection.RecommendedMissionProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MissionRecommendationQueryRepository extends Repository<Mission, Long> {

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
}
