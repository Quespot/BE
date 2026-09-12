package com.quespot.domain.notification.dto;

// 추천 쿼리 결과(서비스 내부용).
public record RecommendableMissionDTO(Long missionId, String title, String spotName, Integer rewardPoint) {
}
