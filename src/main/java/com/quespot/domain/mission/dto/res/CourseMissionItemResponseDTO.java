package com.quespot.domain.mission.dto.res;

public record CourseMissionItemResponseDTO(
        Long missionId,
        Integer seq,
        String title,
        String imageUrl,
        Integer rewardPoint,
        boolean completed
) {
}
