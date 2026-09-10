package com.quespot.domain.mission.dto.res;

import java.util.List;

public record MissionSpotMapResponseDTO(
        String regionCode,
        String regionName,
        int totalSpotCount,
        int completedSpotCount,
        long totalMissionCount,
        long completedMissionCount,
        List<MissionSpotItemResponseDTO> missionSpots
) {
}
