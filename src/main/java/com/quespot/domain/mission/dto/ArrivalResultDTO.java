package com.quespot.domain.mission.dto;

import com.quespot.domain.mission.enums.MissionAttemptStatus;

public record ArrivalResultDTO(
        boolean success,
        long distanceMeters,
        int radiusMeters,
        MissionAttemptStatus status,
        Integer earnedPoint
) {
}
