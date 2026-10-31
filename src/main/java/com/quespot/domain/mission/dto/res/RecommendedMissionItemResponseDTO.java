package com.quespot.domain.mission.dto.res;

import com.quespot.domain.mission.enums.MissionCategory;
import io.swagger.v3.oas.annotations.media.Schema;

public record RecommendedMissionItemResponseDTO(
        Long missionId,
        String title,
        MissionCategory category,
        String spotName,
        @Schema(description = "대표 이미지가 없으면 null", nullable = true)
        String imageUrl,
        @Schema(description = "요청에 latitude/longitude를 보내지 않으면 null", nullable = true)
        Long distanceMeters,
        Integer rewardPoint,
        Integer estimatedMinutes,
        boolean liked
) {
}
