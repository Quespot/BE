package com.quespot.domain.mission.dto.res;

import java.math.BigDecimal;

public record VerificationGuideResponseDTO(
        Long attemptId,
        BigDecimal targetLatitude,
        BigDecimal targetLongitude,
        int radiusMeters
) {
}
