package com.quespot.domain.mission.dto.res;

import java.util.List;

public record NearbyMissionSpotListResponseDTO(
        List<MissionSpotItemResponseDTO> missionSpots
) {
}
