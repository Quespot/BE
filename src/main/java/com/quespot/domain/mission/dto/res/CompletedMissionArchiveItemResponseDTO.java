package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompletedMissionArchiveItemResponseDTO(
        Long missionId,
        @Schema(description = "미션 발행 시 저장된 Mission.snapshotName 값")
        String spotName,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime completedAt,
        Integer earnedPoint
) {
}
