package com.quespot.domain.mission.dto.res;

import com.quespot.domain.mission.enums.CourseAttemptStatus;

import java.time.LocalDateTime;

public record CourseAttemptResponseDTO(
        Long courseAttemptId,
        Long courseId,
        String courseName,
        CourseAttemptStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Integer earnedBonusPoint
) {
}
