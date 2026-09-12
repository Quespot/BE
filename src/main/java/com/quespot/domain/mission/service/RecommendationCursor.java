package com.quespot.domain.mission.service;

record RecommendationCursor(
        SortMode sortMode,
        long seed,
        int preferenceRank,
        long categoryRank,
        long categoryOrder,
        double sortValue,
        long missionId,
        String querySignature
) {
    enum SortMode {
        DISTANCE,
        RANDOM
    }
}
