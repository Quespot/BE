package com.quespot.domain.mission.dto.res;

import com.quespot.domain.mission.enums.CourseAttemptStatus;

import java.util.List;

public record MissionCourseDetailResponseDTO(
        Long courseId,
        String name,
        String description,
        String coverImageUrl,
        String regionCode,
        Integer totalRewardPoint,
        Integer bonusPoint,
        Integer estimatedMinutes,
        List<CourseMissionItemResponseDTO> missions,
        CourseAttemptStatus myStatus
) {
}
