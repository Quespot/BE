package com.quespot.domain.mission.dto.res;

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
        Long distanceMeters
) {
}
