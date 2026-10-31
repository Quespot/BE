package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.mission.enums.CourseAttemptStatus;

import java.util.List;

public record MissionCourseDetailResponseDTO(
        Long courseId,
        String name,
        @Schema(description = "없으면 null", nullable = true)
        String description,
        @Schema(description = "없으면 null", nullable = true)
        String coverImageUrl,
        @Schema(description = "시도 코드(예: 11=서울). 없으면 null", nullable = true)
        String regionCode,
        Integer totalRewardPoint,
        Integer bonusPoint,
        Integer estimatedMinutes,
        @Schema(description = "seq 순. 비면 빈 배열이다(null 아님)")
        List<CourseMissionItemResponseDTO> missions,
        @Schema(description = "내 코스 진행 상태. 시작한 적 없으면 null(생성 응답에서는 항상 IN_PROGRESS)", nullable = true)
        CourseAttemptStatus myStatus
) {
}
