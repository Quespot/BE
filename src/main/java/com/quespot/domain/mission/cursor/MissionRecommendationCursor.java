package com.quespot.domain.mission.cursor;

public record MissionRecommendationCursor(
        SortMode sortMode,
        long seed,
        int preferenceRank,
        long categoryRank,
        long categoryOrder,
        double sortValue,
        long missionId,
        String querySignature
) {
    public enum SortMode {
        DISTANCE,
        RANDOM
    }
}
