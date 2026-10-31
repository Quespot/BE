package com.quespot.domain.reward.dto.res;

public record PointResponseDTO(
        Integer balance,
        Integer totalEarned,
        Integer totalSpent
) {
}
