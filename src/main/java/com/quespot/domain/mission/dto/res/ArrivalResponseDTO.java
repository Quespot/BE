package com.quespot.domain.mission.dto.res;

import com.quespot.domain.mission.enums.MissionAttemptStatus;

public record ArrivalResponseDTO(
        boolean success,
        long distanceMeters,
        int radiusMeters,
        MissionAttemptStatus status,
        Integer earnedPoint
) {
}
