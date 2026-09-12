package com.quespot.domain.mission.cursor;

public record MissionListCursor(
        SortMode sortMode,
        long seed,
        double sortValue,
        long missionId,
        String querySignature
) {
    public enum SortMode {
        DISTANCE,
        RANDOM
    }
}
