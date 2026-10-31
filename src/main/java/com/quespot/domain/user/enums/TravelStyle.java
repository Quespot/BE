package com.quespot.domain.user.enums;

import com.quespot.domain.mission.enums.MissionCategory;

public enum TravelStyle {
    HISTORY(MissionCategory.HISTORY),
    CULTURE(MissionCategory.CULTURE),
    NATURE(MissionCategory.NATURE),
    FOOD(MissionCategory.FOOD),
    NIGHT_VIEW(MissionCategory.NIGHT_VIEW),
    ETC(MissionCategory.ETC);

    private final MissionCategory missionCategory;

    TravelStyle(MissionCategory missionCategory) {
        this.missionCategory = missionCategory;
    }

    public MissionCategory toMissionCategory() {
        return missionCategory;
    }
}
