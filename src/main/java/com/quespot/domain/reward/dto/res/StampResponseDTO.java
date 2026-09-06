package com.quespot.domain.reward.dto.res;

import java.time.LocalDateTime;

public record StampResponseDTO(
        Long id,
        String code,
        String name,
        String regionCode,
        String iconUrl,
        boolean isActive,
        boolean acquired,
        LocalDateTime acquiredAt
) {
}
