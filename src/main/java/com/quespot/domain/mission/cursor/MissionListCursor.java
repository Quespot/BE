package com.quespot.domain.mission.cursor;

// radiusStep: 거리순 조회에서 마지막 행을 찾은 탐색 반경 단계 (MissionSearchRadius 기준, 무작위 정렬은 0)
public record MissionListCursor(
        SortMode sortMode,
        long seed,
        double sortValue,
        long missionId,
        int radiusStep,
        String querySignature
) {
    public enum SortMode {
        DISTANCE,
        RANDOM
    }
}
