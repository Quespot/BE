package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.mission.enums.CourseAttemptStatus;

import java.time.LocalDateTime;

public record CourseAttemptResponseDTO(
        Long courseAttemptId,
        Long courseId,
        String courseName,
        CourseAttemptStatus status,
        LocalDateTime startedAt,
        @Schema(description = "완주 전이면 null", nullable = true)
        LocalDateTime completedAt,
        @Schema(description = "완주 전이면 null", nullable = true)
        Integer earnedBonusPoint
) {
}
