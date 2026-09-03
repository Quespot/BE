package com.quespot.domain.reward.dto.res;

import java.util.List;

public record RewardActivityListResponseDTO(
        List<RewardActivityResponseDTO> activities,
        Long nextCursor,
        boolean hasNext
) {
}
