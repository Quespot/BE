package com.quespot.domain.mission.dto.res;

public record MissionCourseListItemResponseDTO(
        Long courseId,
        String name,
        String coverImageUrl,
        String regionCode,
        Integer totalRewardPoint,
        Integer bonusPoint,
        Integer missionCount,
        Integer estimatedMinutes
) {
}
