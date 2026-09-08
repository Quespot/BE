package com.quespot.domain.mission.dto.res;

import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.UserMissionStatus;

public record MissionListItemResponseDTO(
        Long missionId,
        String title,
        MissionCategory category,
        String spotName,
        String address,
        String imageUrl,
        Long distanceMeters,
        Integer rewardPoint,
        Integer estimatedMinutes,
        UserMissionStatus userMissionStatus,
        boolean canStart
) {
}
