package com.quespot.domain.mission.service;

record MissionCursor(
        SortMode sortMode,
        long seed,
        double sortValue,
        long missionId,
        String querySignature
) {
    enum SortMode {
        DISTANCE,
        RANDOM
    }
}
