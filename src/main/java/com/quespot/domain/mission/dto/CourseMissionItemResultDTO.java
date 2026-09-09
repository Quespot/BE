package com.quespot.domain.mission.dto;

import com.quespot.domain.mission.entity.CourseMission;
import com.quespot.domain.mission.enums.UserMissionStatus;

public record CourseMissionItemResultDTO(
        CourseMission courseMission,
        UserMissionStatus status
) {
}
