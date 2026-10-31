package com.quespot.domain.mission.dto.res;

public record MissionCandidateGenerationResponseDTO(
        int eligibleSpotCount,
        int createdCount,
        int skippedDuplicateCount
) {
}
