package com.quespot.domain.like.enums;

// 장소(SPOT)는 없앴다 — 미션이 곧 장소가 돼서 장소 좋아요가 의미 없어짐(#50).
// docs/quespot_schema.sql의 likes.target_type 주석(MISSION/SPOT/COURSE)은 그래서 stale.
public enum LikeTargetType {
    MISSION,
    COURSE
}
