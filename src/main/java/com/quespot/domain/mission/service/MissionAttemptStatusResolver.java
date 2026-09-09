package com.quespot.domain.mission.service;

import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.UserMissionStatus;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.projection.MissionAttemptStatusProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// MissionQueryService.getMissions()가 미션 목록 한 페이지를 확정한 뒤
// 이 클래스를 한 번만 호출해서 N+1을 피한다 — 미션마다 따로 조회하지 않는다.
@Component
@RequiredArgsConstructor
public class MissionAttemptStatusResolver {

    private final MissionAttemptRepository missionAttemptRepository;

    @Transactional(readOnly = true)
    public Map<Long, UserMissionStatus> resolveStatuses(Long userId, List<Long> missionIds) {
        List<MissionAttemptStatusProjection> rows = missionAttemptRepository.findByUserIdAndMissionIdInAndStatusIn(
                userId, missionIds, List.of(MissionAttemptStatus.COMPLETED, MissionAttemptStatus.IN_PROGRESS)
        );

        Map<Long, UserMissionStatus> statuses = new HashMap<>();
        for (MissionAttemptStatusProjection row : rows) {
            // 재시작 차단 정책 덕분에 COMPLETED는 missionId당 최대 1건, active_key
            // 덕분에 IN_PROGRESS도 최대 1건이지만, 혹시 둘 다 있으면 COMPLETED가 이긴다.
            UserMissionStatus resolved = row.getStatus() == MissionAttemptStatus.COMPLETED
                    ? UserMissionStatus.COMPLETED
                    : UserMissionStatus.IN_PROGRESS;
            statuses.merge(row.getMissionId(), resolved,
                    (existing, candidate) -> existing == UserMissionStatus.COMPLETED ? existing : candidate);
        }
        return statuses;
    }

    public UserMissionStatus resolveStatus(Long userId, Long missionId) {
        return resolveStatuses(userId, List.of(missionId)).getOrDefault(missionId, UserMissionStatus.AVAILABLE);
    }
}
