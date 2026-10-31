package com.quespot.domain.reward.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record BadgeResponseDTO(
        Long id,
        String code,
        String name,
        @Schema(description = "없으면 null", nullable = true)
        String description,
        @Schema(description = "아이콘이 아직 없으면 null", nullable = true)
        String iconUrl,
        boolean acquired,
        @Schema(description = "미획득(acquired=false)이면 null", nullable = true)
        LocalDateTime acquiredAt
) {
}
