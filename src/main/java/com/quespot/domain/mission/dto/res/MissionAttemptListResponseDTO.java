package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MissionAttemptListResponseDTO(
        @Schema(description = "비면 빈 배열이다(null 아님)")
        List<MissionAttemptResponseDTO> attempts
) {
}
