package com.quespot.domain.mission.repository.projection;

import com.quespot.domain.mission.enums.MissionAttemptStatus;

public interface MissionAttemptStatusProjection {

    Long getMissionId();

    MissionAttemptStatus getStatus();
}
