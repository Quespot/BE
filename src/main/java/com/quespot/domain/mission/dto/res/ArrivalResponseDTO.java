package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.mission.enums.MissionAttemptStatus;

public record ArrivalResponseDTO(
        @Schema(description = "반경 500m 안이면 true. false여도 HTTP 200이며 상태는 바뀌지 않는다")
        boolean success,
        @Schema(description = "현재 위치와 미션 좌표 사이 거리(m). 실패 시 안내 문구에 쓴다")
        long distanceMeters,
        @Schema(description = "인증 반경(m). 현재 500 고정")
        int radiusMeters,
        @Schema(description = "판정 후 시도 상태. 성공 시 COMPLETED, 실패 시 IN_PROGRESS 그대로")
        MissionAttemptStatus status,
        @Schema(description = "성공 시 지급된 포인트. 실패(success=false) 시 null. 이미 완료된 시도에 재호출하면 0", nullable = true)
        Integer earnedPoint
) {
}
