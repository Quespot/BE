package com.quespot.domain.reward.dto.res;

import java.util.List;

public record BadgeListResponseDTO(
        List<BadgeResponseDTO> badges
) {
}
