package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.repository.projection.CompletedMissionArchiveProjection;
import com.quespot.domain.mission.repository.projection.MissionAttemptStatusProjection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MissionAttemptRepository extends JpaRepository<MissionAttempt, Long> {

    Optional<MissionAttempt> findByUserIdAndMissionIdAndStatus(
            Long userId, Long missionId, MissionAttemptStatus status
    );

    @EntityGraph(attributePaths = "mission")
    List<MissionAttempt> findByUserIdAndStatus(Long userId, MissionAttemptStatus status);

    // 목록 조회 화면에서 미션마다 시도를 따로 조회하지 않기 위한 벌크 조회.
    // 재시작 차단 정책 덕분에 (user, mission) 당 COMPLETED는 최대 1건,
    // active_key 덕분에 IN_PROGRESS도 최대 1건이라 결과가 작다.
    List<MissionAttemptStatusProjection> findByUserIdAndMissionIdInAndStatusIn(
            Long userId, List<Long> missionIds, List<MissionAttemptStatus> statuses
    );

    @Query(value = """
            select
                m.id as missionId,
                m.snapshot_name as spotName,
                m.snapshot_latitude as latitude,
                m.snapshot_longitude as longitude,
                ma.completed_at as completedAt,
                ma.earned_point as earnedPoint
            from mission_attempts ma
            join missions m on m.id = ma.mission_id
            where ma.user_id = :userId
              and ma.status = 'COMPLETED'
              and (:startAt is null or ma.completed_at >= :startAt)
              and (:endAt is null or ma.completed_at < :endAt)
              and (:regionCode is null or left(m.snapshot_district_code, 2) = :regionCode)
              and (:category is null or m.category = :category)
            order by ma.completed_at desc, ma.id desc
            """, nativeQuery = true)
    List<CompletedMissionArchiveProjection> findCompletedMissionArchive(
            @Param("userId") Long userId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("regionCode") String regionCode,
            @Param("category") String category
    );

    // 코스 포기 시 그 코스에 연결된 모든 미션 시도를 찾아 courseAttemptId를
    // null로 비우는 데 쓴다(#39).
    List<MissionAttempt> findByCourseAttemptId(Long courseAttemptId);
}
