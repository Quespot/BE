package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.CourseAttempt;
import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.enums.CourseAttemptStatus;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.MissionStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
import com.quespot.domain.mission.repository.CourseAttemptRepository;
import com.quespot.domain.mission.repository.CourseMissionRepository;
import com.quespot.domain.mission.repository.MissionAttemptRepository;
import com.quespot.domain.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MissionAttemptService {

    private final MissionAttemptRepository missionAttemptRepository;
    private final MissionRepository missionRepository;
    private final CourseAttemptRepository courseAttemptRepository;
    private final CourseMissionRepository courseMissionRepository;
    private final CourseLockPolicy courseLockPolicy;

    @Transactional
    public MissionAttempt start(Long userId, Long missionId) {
        return start(userId, missionId, null);
    }

    @Transactional
    public MissionAttempt start(Long userId, Long missionId, Long courseAttemptId) {
        Mission mission = missionRepository.findByIdAndStatus(missionId, MissionStatus.ACTIVE)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        // courseAttemptId 유무와 무관하게 항상 실행 — 미션 목록에서 courseAttemptId
        // 없이 직접 시작을 눌러도 잠긴 미션이면 막는다(#39 재설계 핵심 요구사항).
        if (courseLockPolicy.resolveLockedMissionIds(userId, java.util.List.of(missionId)).contains(missionId)) {
            throw new MissionException(MissionErrorCode.MISSION_LOCKED);
        }

        if (courseAttemptId != null) {
            validateMissionBelongsToCourse(userId, courseAttemptId, missionId);
        }

        return missionAttemptRepository
                .findByUserIdAndMissionIdAndStatus(userId, missionId, MissionAttemptStatus.IN_PROGRESS)
                .map(attempt -> attachToCourseIfNeeded(attempt, courseAttemptId))
                .orElseGet(() -> createNewAttempt(userId, missionId, mission, courseAttemptId));
    }

    // 이미 진행 중인 attempt를 코스 컨텍스트 없이 시작해 뒀다가, 나중에 같은 미션을
    // 코스 화면에서 다시 시작하는 경우 courseAttemptId가 붙지 않으면 완주 판정
    // 트리거(MissionArrivalService.arrive()의 courseAttemptId != null 체크)가
    // 영영 걸리지 않는다. 붙어있지 않을 때만 채운다 — 이미 다른 코스에 연결돼
    // 있으면 그 연결을 임의로 덮어쓰지 않고 에러로 알린다.
    private MissionAttempt attachToCourseIfNeeded(MissionAttempt attempt, Long courseAttemptId) {
        if (courseAttemptId == null) {
            return attempt;
        }
        Long existing = attempt.getCourseAttemptId();
        if (existing == null) {
            attempt.attachToCourse(courseAttemptId);
        } else if (!existing.equals(courseAttemptId)) {
            throw new MissionException(MissionErrorCode.MISSION_NOT_IN_COURSE);
        }
        return attempt;
    }

    private void validateMissionBelongsToCourse(Long userId, Long courseAttemptId, Long missionId) {
        CourseAttempt courseAttempt = courseAttemptRepository.findById(courseAttemptId)
                .filter(ca -> ca.getUserId().equals(userId))
                .orElseThrow(() -> new MissionException(MissionErrorCode.COURSE_ATTEMPT_NOT_FOUND));
        if (courseAttempt.getStatus() != CourseAttemptStatus.IN_PROGRESS) {
            throw new MissionException(MissionErrorCode.COURSE_ATTEMPT_NOT_IN_PROGRESS);
        }
        boolean belongsToCourse = courseMissionRepository
                .existsByCourseIdAndMissionId(courseAttempt.getCourse().getId(), missionId);
        if (!belongsToCourse) {
            throw new MissionException(MissionErrorCode.MISSION_NOT_IN_COURSE);
        }
    }

    private MissionAttempt createNewAttempt(Long userId, Long missionId, Mission mission, Long courseAttemptId) {
        boolean alreadyCompleted = missionAttemptRepository
                .findByUserIdAndMissionIdAndStatus(userId, missionId, MissionAttemptStatus.COMPLETED)
                .isPresent();
        if (alreadyCompleted) {
            throw new MissionException(MissionErrorCode.MISSION_ALREADY_COMPLETED);
        }

        return missionAttemptRepository.save(MissionAttempt.start(userId, mission, courseAttemptId));
    }

    @Transactional(readOnly = true)
    public java.util.List<MissionAttempt> getInProgressList(Long userId) {
        return missionAttemptRepository.findByUserIdAndStatus(userId, MissionAttemptStatus.IN_PROGRESS);
    }

    @Transactional(readOnly = true)
    public MissionAttempt getAttempt(Long userId, Long attemptId) {
        return findOwnedAttempt(userId, attemptId);
    }

    @Transactional
    public void quit(Long userId, Long attemptId) {
        MissionAttempt attempt = findOwnedAttempt(userId, attemptId);
        if (attempt.getStatus() != MissionAttemptStatus.IN_PROGRESS) {
            throw new MissionException(MissionErrorCode.ATTEMPT_NOT_IN_PROGRESS);
        }
        attempt.quit();
    }

    @Transactional
    public void writeReflection(Long userId, Long attemptId, String content) {
        MissionAttempt attempt = findOwnedAttempt(userId, attemptId);
        attempt.writeReflection(content);
    }

    private MissionAttempt findOwnedAttempt(Long userId, Long attemptId) {
        return missionAttemptRepository.findById(attemptId)
                .filter(a -> a.getUserId().equals(userId))
                .orElseThrow(() -> new MissionException(MissionErrorCode.ATTEMPT_NOT_FOUND));
    }
}
