package com.quespot.domain.mission.service;

import com.quespot.domain.mission.dto.CourseMissionItemResultDTO;
import com.quespot.domain.mission.entity.CourseMission;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.UserMissionStatus;
import com.quespot.domain.mission.repository.CourseMissionRepository;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.projection.CourseMissionLockRowProjection;
import com.quespot.domain.mission.repository.projection.MissionAttemptStatusProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 잠금 판정의 유일한 진실 — 코스 상세/미션 목록/미션 시작 세 곳이 전부 이 규칙을
// 공유한다(#39 재설계). 로직이 흩어지면 한 곳만 고치는 실수가 나기 때문.
@Component
@RequiredArgsConstructor
public class CourseLockPolicy {

    private final CourseMissionRepository courseMissionRepository;
    private final MissionAttemptRepository missionAttemptRepository;

    private boolean isLocked(int seq, boolean previousSeqCompleted) {
        return seq > 1 && !previousSeqCompleted;
    }

    // 코스 상세용 — 이미 조회된 데이터로 seq 순서대로 계산. 추가 쿼리 없음.
    public List<CourseMissionItemResultDTO> resolveCourseMissionStatuses(
            List<CourseMission> orderedBySeq, Set<Long> completedMissionIds, Set<Long> inProgressMissionIds
    ) {
        List<CourseMissionItemResultDTO> result = new ArrayList<>();
        boolean previousCompleted = true;
        for (CourseMission cm : orderedBySeq) {
            Long missionId = cm.getMission().getId();
            UserMissionStatus status;
            if (completedMissionIds.contains(missionId)) {
                status = UserMissionStatus.COMPLETED;
            } else if (inProgressMissionIds.contains(missionId)) {
                status = UserMissionStatus.IN_PROGRESS;
            } else if (!isLocked(cm.getSeq(), previousCompleted)) {
                status = UserMissionStatus.AVAILABLE;
            } else {
                status = UserMissionStatus.LOCKED;
            }
            result.add(new CourseMissionItemResultDTO(cm, status));
            previousCompleted = completedMissionIds.contains(missionId);
        }
        return result;
    }

    // 목록/시작 우회 차단용 — userId의 IN_PROGRESS 코스들 중 missionIds가 속한 것만
    // 찾아 잠금 계산. 쿼리 2개 고정(N+1 없음). 같은 미션이 여러 진행 중 코스에 걸려
    // 있으면 하나라도 잠금 해제돼 있으면 허용한다.
    public Set<Long> resolveLockedMissionIds(Long userId, List<Long> missionIds) {
        List<CourseMissionLockRowProjection> rows =
                courseMissionRepository.findLockRowsForInProgressCourses(userId, missionIds);
        if (rows.isEmpty()) {
            return Set.of();
        }

        Set<Long> prevMissionIds = new HashSet<>();
        for (CourseMissionLockRowProjection row : rows) {
            if (row.getPrevMissionId() != null) {
                prevMissionIds.add(row.getPrevMissionId());
            }
        }
        Set<Long> completedPrevIds = new HashSet<>();
        if (!prevMissionIds.isEmpty()) {
            List<MissionAttemptStatusProjection> completed = missionAttemptRepository
                    .findByUserIdAndMissionIdInAndStatusIn(
                            userId, List.copyOf(prevMissionIds), List.of(MissionAttemptStatus.COMPLETED)
                    );
            completed.forEach(p -> completedPrevIds.add(p.getMissionId()));
        }

        Map<Long, Boolean> lockedByMission = new HashMap<>();
        for (CourseMissionLockRowProjection row : rows) {
            boolean lockedHere = row.getPrevMissionId() != null && !completedPrevIds.contains(row.getPrevMissionId());
            lockedByMission.merge(row.getMissionId(), lockedHere, (a, b) -> a && b);
        }

        Set<Long> locked = new HashSet<>();
        lockedByMission.forEach((missionId, isLockedFlag) -> {
            if (isLockedFlag) {
                locked.add(missionId);
            }
        });
        return locked;
    }
}
