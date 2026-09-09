package com.quespot.domain.mission.dto.res;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MissionPhotoResponseDTO(
        Long photoId,
        String imageUrl,
        String caption,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime takenAt
) {
}
