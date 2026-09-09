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
import java.util.Set;

// MissionQueryService.getMissions()가 미션 목록 한 페이지를 확정한 뒤
// 이 클래스를 한 번만 호출해서 N+1을 피한다 — 미션마다 따로 조회하지 않는다.
// #39 재설계로 CourseLockPolicy를 통한 LOCKED 오버레이가 추가됐지만, 시그니처는
// 그대로라 MissionQueryService.java는 변경이 필요 없다.
@Component
@RequiredArgsConstructor
public class MissionAttemptStatusResolver {

    private final MissionAttemptRepository missionAttemptRepository;
    private final CourseLockPolicy courseLockPolicy;

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

        List<Long> remaining = missionIds.stream().filter(id -> !statuses.containsKey(id)).toList();
        if (!remaining.isEmpty()) {
            Set<Long> lockedIds = courseLockPolicy.resolveLockedMissionIds(userId, remaining);
            for (Long id : lockedIds) {
                statuses.put(id, UserMissionStatus.LOCKED);
            }
        }
        return statuses;
    }

    public UserMissionStatus resolveStatus(Long userId, Long missionId) {
        return resolveStatuses(userId, List.of(missionId)).getOrDefault(missionId, UserMissionStatus.AVAILABLE);
    }
}
