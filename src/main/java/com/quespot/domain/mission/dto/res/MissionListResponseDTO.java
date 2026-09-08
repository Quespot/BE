package com.quespot.domain.mission.dto.res;

import java.util.List;

public record MissionListResponseDTO(
        List<MissionListItemResponseDTO> missions,
        String nextCursor,
        boolean hasNext
) {
}
