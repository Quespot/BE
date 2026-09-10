package com.quespot.domain.mission.dto.res;

import java.util.List;

public record MissionArchiveListResponseDTO(
        List<MissionArchiveItemResponseDTO> archives,
        String nextCursor,
        boolean hasNext
) {
}
