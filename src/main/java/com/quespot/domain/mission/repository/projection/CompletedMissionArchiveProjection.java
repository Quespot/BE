package com.quespot.domain.mission.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CompletedMissionArchiveProjection {

    Long getMissionId();

    String getSpotName();

    BigDecimal getLatitude();

    BigDecimal getLongitude();

    LocalDateTime getCompletedAt();

    Integer getEarnedPoint();
}
