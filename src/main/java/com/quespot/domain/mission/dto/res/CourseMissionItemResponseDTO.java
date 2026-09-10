package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.mission.enums.UserMissionStatus;

public record CourseMissionItemResponseDTO(
        Long missionId,
        Integer seq,
        String title,
        @Schema(description = "없으면 null", nullable = true)
        String imageUrl,
        Integer rewardPoint,
        @Schema(description = "LOCKED = 앞 순서 미완료(시작 불가), AVAILABLE = 지금 시작 가능, IN_PROGRESS = 진행 중, COMPLETED = 완료. 앞 미션을 완료하면 다음 하나만 AVAILABLE로 바뀐다")
        UserMissionStatus status
) {
}
