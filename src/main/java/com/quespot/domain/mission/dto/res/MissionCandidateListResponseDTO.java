package com.quespot.domain.mission.dto.res;

import java.util.List;

public record MissionCandidateListResponseDTO(
        List<MissionCandidateResponseDTO> candidates,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
