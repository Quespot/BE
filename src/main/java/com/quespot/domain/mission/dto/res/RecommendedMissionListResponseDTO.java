package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RecommendedMissionListResponseDTO(
        @Schema(description = "바로 시작할 수 있는 추천 미션. 비면 빈 배열이다(null 아님)")
        List<RecommendedMissionItemResponseDTO> missions,
        @Schema(description = "다음 요청의 cursor에 그대로 넣는다. 마지막 페이지면 null", nullable = true)
        String nextCursor,
        boolean hasNext
) {
}
