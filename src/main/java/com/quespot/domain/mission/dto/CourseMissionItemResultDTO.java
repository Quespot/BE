package com.quespot.domain.mission.dto;

import com.quespot.domain.mission.entity.CourseMission;

public record CourseMissionItemResultDTO(
        CourseMission courseMission,
        boolean completed
) {
}
