package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
public record UnlockConditionResponseDTO(
        @Schema(description = "true면 진행 중 코스의 앞 미션이 미완료라 시작할 수 없다")
        boolean locked,
        @Schema(description = "잠긴 이유 안내 문구. locked=false면 null", nullable = true)
        String message
) {
}
