package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.UserMissionStatus;

public record MissionListItemResponseDTO(
        Long missionId,
        String title,
        MissionCategory category,
        String spotName,
        @Schema(description = "원천에 주소가 없으면 null", nullable = true)
        String address,
        @Schema(description = "대표 이미지가 없으면 null", nullable = true)
        String imageUrl,
        @Schema(description = "요청에 latitude/longitude를 보냈을 때만 채워진다. 안 보냈으면 null", nullable = true)
        Long distanceMeters,
        Integer rewardPoint,
        Integer estimatedMinutes,
        @Schema(description = "AVAILABLE / IN_PROGRESS / COMPLETED / LOCKED. LOCKED는 진행 중 코스에서 앞 미션이 미완료라는 뜻")
        UserMissionStatus userMissionStatus,
        @Schema(description = "userMissionStatus=AVAILABLE일 때만 true. false 사유는 userMissionStatus로 판단")
        boolean canStart
) {
}
