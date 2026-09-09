package com.quespot.domain.mission.dto.res;

import com.quespot.domain.mission.enums.MissionAttemptStatus;

import java.time.LocalDateTime;

public record MissionAttemptResponseDTO(
        Long attemptId,
        Long missionId,
        String missionTitle,
        MissionAttemptStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Integer earnedPoint
) {
}
