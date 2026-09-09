package com.quespot.domain.mission.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MissionCourseGenerateRequestDTO(
        @NotNull @Positive Long anchorMissionId
) {
}
