package com.quespot.domain.like.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.mission.enums.MissionCategory;

import java.time.LocalDateTime;

// 거리·평점은 없다 — 거리는 lat/lng 파라미터가 필요하고 평점은 출처 미정(CLAUDE.md).
public record LikedMissionResponseDTO(
        Long missionId,
        String title,
        MissionCategory category,
        String spotName,
        @Schema(description = "없으면 null", nullable = true)
        String imageUrl,
        Integer rewardPoint,
        Integer estimatedMinutes,
        LocalDateTime likedAt
) {
}
