package com.quespot.domain.mission.dto.res;

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
        String reason,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
