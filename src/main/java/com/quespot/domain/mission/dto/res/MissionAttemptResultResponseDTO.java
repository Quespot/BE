package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record MissionAttemptResultResponseDTO(
        Long attemptId,
        String missionTitle,
        @Schema(description = "완료 전이면 null", nullable = true)
        Integer earnedPoint,
        @Schema(description = "완료 전이면 null", nullable = true)
        LocalDateTime completedAt,
        @Schema(description = "사진을 등록하지 않았으면 null. 등록했으면 매번 새로 서명한 presigned URL(만료됨, 저장하지 말 것)", nullable = true)
        String photoUrl
) {
}
