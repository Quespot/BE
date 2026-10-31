package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.mission.enums.MissionAttemptStatus;

import java.time.LocalDateTime;

public record MissionAttemptResponseDTO(
        Long attemptId,
        Long missionId,
        String missionTitle,
        MissionAttemptStatus status,
        LocalDateTime startedAt,
        @Schema(description = "완료 전이면 null", nullable = true)
        LocalDateTime completedAt,
        @Schema(description = "완료 전이면 null", nullable = true)
        Integer earnedPoint
) {
}
