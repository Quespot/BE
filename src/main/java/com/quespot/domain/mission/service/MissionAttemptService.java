package com.quespot.domain.mission.service;

import com.quespot.domain.mission.entity.Mission;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.enums.MissionAttemptStatus;
import com.quespot.domain.mission.enums.MissionStatus;
import com.quespot.domain.mission.exception.MissionException;
import com.quespot.domain.mission.exception.code.MissionErrorCode;
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

    @Transactional
    public MissionAttempt start(Long userId, Long missionId) {
        Mission mission = missionRepository.findByIdAndStatus(missionId, MissionStatus.ACTIVE)
                .orElseThrow(() -> new MissionException(MissionErrorCode.MISSION_NOT_FOUND));

        return missionAttemptRepository
                .findByUserIdAndMissionIdAndStatus(userId, missionId, MissionAttemptStatus.IN_PROGRESS)
                .orElseGet(() -> createNewAttempt(userId, missionId, mission));
    }

    private MissionAttempt createNewAttempt(Long userId, Long missionId, Mission mission) {
        boolean alreadyCompleted = missionAttemptRepository
                .findByUserIdAndMissionIdAndStatus(userId, missionId, MissionAttemptStatus.COMPLETED)
                .isPresent();
        if (alreadyCompleted) {
            throw new MissionException(MissionErrorCode.MISSION_ALREADY_COMPLETED);
        }

        return missionAttemptRepository.save(MissionAttempt.start(userId, mission));
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
