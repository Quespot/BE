package com.quespot.domain.reward.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record StampResponseDTO(
        Long id,
        String code,
        String name,
        String regionCode,
        @Schema(description = "아이콘이 아직 없으면 null", nullable = true)
        String iconUrl,
        @Schema(description = "수집 가능 지역이면 true(현재 서울만). false는 잠금으로 노출")
        boolean isActive,
        boolean acquired,
        @Schema(description = "미획득(acquired=false)이면 null", nullable = true)
        LocalDateTime acquiredAt
) {
}
