package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MissionArchiveListResponseDTO(
        @Schema(description = "비면 빈 배열이다(null 아님)")
        List<MissionArchiveItemResponseDTO> archives,
        @Schema(description = "다음 요청의 cursor에 그대로 넣는다. 마지막 페이지(hasNext=false)면 null. 파싱·생성 금지", nullable = true)
        String nextCursor,
        boolean hasNext
) {
}
