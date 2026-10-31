package com.quespot.domain.mission.repository.projection;

public interface MissionSpotSummaryProjection {

    String getDistrictCode();

    Long getMissionCount();

    Long getCompletedMissionCount();
}
