package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.UserMissionStatus;

import java.math.BigDecimal;

public record MissionDetailResponseDTO(
        Long missionId,
        String title,
        String description,
        MissionCategory category,
        String spotName,
        @Schema(description = "원천에 주소가 없으면 null", nullable = true)
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        @Schema(description = "대표 이미지가 없으면 null", nullable = true)
        String imageUrl,
        @Schema(description = "요청에 latitude/longitude를 보냈을 때만 채워진다. 안 보냈으면 null", nullable = true)
        Long distanceMeters,
        Integer rewardPoint,
        Integer estimatedMinutes,
        @Schema(description = "AVAILABLE / IN_PROGRESS / COMPLETED / LOCKED")
        UserMissionStatus userMissionStatus,
        @Schema(description = "AVAILABLE일 때만 true. IN_PROGRESS → 진행 중 시도로 이동, COMPLETED → 완료 표시, LOCKED → unlock-condition의 message 표시")
        boolean canStart,
        @Schema(description = "현재 미구현으로 항상 false. 좋아요 여부는 GET /api/users/me/liked-missions로 판단한다")
        boolean liked,
        @Schema(description = "COMPLETED일 때만 true(미션 사진 등록 가능)")
        boolean canCreateArchive
) {
}
