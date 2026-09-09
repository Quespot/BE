package com.quespot.domain.mission.dto.res;

import com.quespot.domain.mission.enums.CourseAttemptStatus;

public record MissionCourseListItemResponseDTO(
        Long courseId,
        String name,
        String coverImageUrl,
        String regionCode,
        Integer totalRewardPoint,
        Integer bonusPoint,
        Integer missionCount,
        Integer estimatedMinutes,
        CourseAttemptStatus myStatus
) {
}
