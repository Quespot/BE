package com.quespot.domain.reward.dto.res;

import java.time.LocalDateTime;

public record BadgeResponseDTO(
        Long id,
        String code,
        String name,
        String description,
        String iconUrl,
        boolean acquired,
        LocalDateTime acquiredAt
) {
}
