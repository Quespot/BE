package com.quespot.domain.mission.repository.projection;

import java.math.BigDecimal;

public interface CourseCandidateMissionProjection {
    Long getMissionId();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
}
