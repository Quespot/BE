package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.mission.enums.MissionCandidateStatus;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.MissionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MissionCandidateResponseDTO(
        Long candidateId,
        Long spotId,
        String spotName,
        String spotAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String imageUrl,
        MissionTemplate templateCode,
        Integer generatorVersion,
        String title,
        String description,
        MissionCategory category,
        Integer rewardPoint,
        Integer estimatedMinutes,
        MissionCandidateStatus status,
        @Schema(description = "반려 사유. 반려 전이면 null", nullable = true)
        String reason,
        @Schema(description = "검수 전이면 null", nullable = true)
        Long reviewedBy,
        @Schema(description = "검수 전이면 null", nullable = true)
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
