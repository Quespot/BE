package com.quespot.domain.reward.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RewardActivityListResponseDTO(
        @Schema(description = "비면 빈 배열이다(null 아님)")
        List<RewardActivityResponseDTO> activities,
        @Schema(description = "다음 요청의 cursor에 그대로 넣는 숫자 id. 마지막 페이지(hasNext=false)면 null", nullable = true)
        Long nextCursor,
        boolean hasNext
) {
}
