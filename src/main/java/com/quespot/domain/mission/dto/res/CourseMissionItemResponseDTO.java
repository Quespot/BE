package com.quespot.domain.mission.dto.res;

import com.quespot.domain.mission.enums.UserMissionStatus;

public record CourseMissionItemResponseDTO(
        Long missionId,
        Integer seq,
        String title,
        String imageUrl,
        Integer rewardPoint,
        UserMissionStatus status
) {
}
