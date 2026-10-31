package com.quespot.domain.mission.repository.projection;

public interface RecommendedMissionProjection extends MissionListProjection {

    Integer getPreferenceRank();

    Long getCategoryRank();

    Long getCategoryOrder();
}
