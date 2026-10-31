package com.quespot.domain.reward.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record BadgeListResponseDTO(
        @Schema(description = "비면 빈 배열이다(null 아님)")
        List<BadgeResponseDTO> badges
) {
}
