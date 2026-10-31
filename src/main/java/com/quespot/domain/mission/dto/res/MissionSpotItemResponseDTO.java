package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.mission.enums.MissionSpotCompletionStatus;

import java.math.BigDecimal;

public record MissionSpotItemResponseDTO(
        String districtCode,
        String districtName,
        BigDecimal latitude,
        BigDecimal longitude,
        long missionCount,
        long completedMissionCount,
        MissionSpotCompletionStatus completionStatus,
        @Schema(description = "주변 조회(/nearby)에서만 채워진다. 행정구역 조회에서는 null", nullable = true)
        Long distanceMeters
) {
}
