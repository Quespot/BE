package com.quespot.domain.mission.dto.res;

public record MissionCandidateBatchResponseDTO(
        int targetCount,
        int processedCount,
        int skippedCount
) {
}
