package com.quespot.domain.reward.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.reward.enums.ActivityType;

import java.time.LocalDateTime;

public record RewardActivityResponseDTO(
        Long id,
        ActivityType activityType,
        String title,
        @Schema(description = "포인트 변동량(적립 +, 사용 -). 배지·스탬프 획득처럼 포인트 변동이 없는 활동이면 null", nullable = true)
        Integer amount,
        LocalDateTime createdAt
) {
}
