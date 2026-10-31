package com.quespot.domain.mission.repository.projection;

public interface NearbyMissionSpotProjection extends MissionSpotSummaryProjection {

    Double getDistanceMeters();
}
