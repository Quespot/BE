package com.quespot.domain.mission.repository.projection;

public interface MissionListProjection {

    Long getMissionId();

    String getTitle();

    String getCategory();

    String getSpotName();

    String getAddress();

    String getImageUrl();

    Integer getRewardPoint();

    Integer getEstimatedMinutes();

    Double getSortValue();
}
