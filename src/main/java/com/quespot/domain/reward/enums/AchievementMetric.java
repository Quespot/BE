package com.quespot.domain.reward.enums;

// badges.condition_json의 metric 값. 새 배지는 이 enum과 AchievementMetricRepository에
// 집계 쿼리를 추가하는 것으로 끝나야 한다 — if문 체인 금지(CLAUDE.md).
public enum AchievementMetric {
    MISSION_COMPLETED,
    MISSION_COMPLETED_DISTINCT_DISTRICT,
    PHOTO_REGISTERED,
    COURSE_COMPLETED
}
