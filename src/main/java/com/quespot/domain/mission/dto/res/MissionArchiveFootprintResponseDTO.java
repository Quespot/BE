package com.quespot.domain.mission.dto.res;

public record MissionArchiveFootprintResponseDTO(
        int completedMissionCount,
        long totalEarnedPoint
) {
}
