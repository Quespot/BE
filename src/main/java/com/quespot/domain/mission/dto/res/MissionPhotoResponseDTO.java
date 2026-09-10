package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MissionPhotoResponseDTO(
        Long photoId,
        @Schema(description = "매번 새로 서명한 presigned URL. 만료되므로 저장하지 말고 조회 때마다 새로 받는다")
        String imageUrl,
        @Schema(description = "사용자가 안 적었으면 null", nullable = true)
        String caption,
        BigDecimal latitude,
        BigDecimal longitude,
        @Schema(description = "클라이언트가 안 보냈으면 null", nullable = true)
        LocalDateTime takenAt
) {
}
