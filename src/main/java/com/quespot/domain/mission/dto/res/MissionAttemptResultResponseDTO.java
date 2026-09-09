package com.quespot.domain.mission.dto.res;

import java.time.LocalDateTime;

public record MissionAttemptResultResponseDTO(
        Long attemptId,
        String missionTitle,
        Integer earnedPoint,
        LocalDateTime completedAt,
        String photoUrl
) {
}
