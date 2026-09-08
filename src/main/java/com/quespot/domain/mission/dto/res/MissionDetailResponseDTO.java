package com.quespot.domain.mission.dto.res;

import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.UserMissionStatus;

import java.math.BigDecimal;

public record MissionDetailResponseDTO(
        Long missionId,
        String title,
        String description,
        MissionCategory category,
        String spotName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String imageUrl,
        Long distanceMeters,
        Integer rewardPoint,
        Integer estimatedMinutes,
        UserMissionStatus userMissionStatus,
        boolean canStart,
        boolean liked,
        boolean canCreateArchive
) {
}
