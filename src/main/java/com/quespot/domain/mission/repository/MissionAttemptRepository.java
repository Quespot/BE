package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.repository.projection.MissionAttemptStatusProjection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
