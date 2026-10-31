package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.mission.enums.CourseAttemptStatus;

public record MissionCourseListItemResponseDTO(
        Long courseId,
        String name,
        @Schema(description = "없으면 null", nullable = true)
        String coverImageUrl,
        @Schema(description = "시도 코드(예: 11=서울). 없으면 null", nullable = true)
        String regionCode,
        Integer totalRewardPoint,
        Integer bonusPoint,
        Integer missionCount,
        Integer estimatedMinutes,
        @Schema(description = "내 코스 진행 상태. 시작한 적 없으면 null", nullable = true)
        CourseAttemptStatus myStatus
) {
}
