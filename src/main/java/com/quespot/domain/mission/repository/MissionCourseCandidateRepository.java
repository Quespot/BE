package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.repository.projection.CourseCandidateMissionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

// 코스 생성 알고리즘 전용 최근접 후보 검색(#39 재설계). MissionRepository(허건우
// 소유, 미션 목록/상세 조회)는 건드리지 않는다 — Mission 엔티티에 대한 두 번째
// JpaRepository는 별도 Spring 빈으로 문제없이 공존한다(#37 GeoDistanceCalculator와
// 동일한 이유).
public interface MissionCourseCandidateRepository extends JpaRepository<Mission, Long> {

    @Query(value = """
            select
                m.id as missionId,
                m.snapshot_latitude as latitude,
                m.snapshot_longitude as longitude
            from missions m
            where m.status = 'ACTIVE'
              and m.category in (:categories)
              and m.id not in (:excludeIds)
              and ST_Distance_Sphere(
                    POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(:refLng, :refLat)
                  ) <= :radiusMeters
              and not exists (
                  select 1 from mission_attempts ma
                  where ma.user_id = :userId and ma.mission_id = m.id
                    and ma.status in ('IN_PROGRESS','COMPLETED')
              )
            order by ST_Distance_Sphere(
                    POINT(m.snapshot_longitude, m.snapshot_latitude), POINT(:refLng, :refLat)
                  ) asc, m.id asc
            limit :limit
            """, nativeQuery = true)
    List<CourseCandidateMissionProjection> findNearestCandidates(
            @Param("refLat") BigDecimal refLat,
            @Param("refLng") BigDecimal refLng,
            @Param("radiusMeters") int radiusMeters,
            @Param("categories") List<String> categories,
            @Param("excludeIds") List<Long> excludeIds,
            @Param("userId") Long userId,
            @Param("limit") int limit
    );
}
