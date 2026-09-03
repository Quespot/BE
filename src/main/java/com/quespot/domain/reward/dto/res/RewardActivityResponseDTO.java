package com.quespot.domain.reward.dto.res;

import com.quespot.domain.reward.enums.ActivityType;

import java.time.LocalDateTime;

public record RewardActivityResponseDTO(
        Long id,
        ActivityType activityType,
        String title,
        Integer amount,
        LocalDateTime createdAt
) {
}
