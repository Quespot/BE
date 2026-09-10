package com.quespot.domain.like.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

// totalCount는 노출 건수(비활성 미션 제외) — 헤더의 "3개"를 서버가 센다.
public record LikedMissionListResponseDTO(
        @Schema(description = "비면 빈 배열이다(null 아님)")
        List<LikedMissionResponseDTO> items,
        int totalCount
) {
}
