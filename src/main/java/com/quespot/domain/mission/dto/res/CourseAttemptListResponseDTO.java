package com.quespot.domain.mission.dto.res;

import java.util.List;

public record CourseAttemptListResponseDTO(
        List<CourseAttemptResponseDTO> attempts
) {
}
