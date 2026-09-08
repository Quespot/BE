package com.quespot.domain.tour.dto.res;

import com.quespot.domain.mission.dto.res.MissionCandidateGenerationResponseDTO;
import com.quespot.domain.spot.service.RefinementSummary;

public record TourSyncResponseDTO(
        RefinementSummary refinement,
        MissionCandidateGenerationResponseDTO candidateGeneration
) {
}
