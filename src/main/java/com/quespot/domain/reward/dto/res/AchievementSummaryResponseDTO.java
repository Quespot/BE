package com.quespot.domain.reward.dto.res;

// 홈 상단 4개 숫자와 마이페이지 달성 현황이 같이 쓴다. 분모(totalBadgeCount=
// is_active 배지 수, totalStampCount=전체 8개 슬롯)는 서버가 센다 — 클라이언트
// 하드코딩(6/8 불일치) 방지. 완료 미션 분모(2/20의 20)는 미정이라 내리지 않는다.
public record AchievementSummaryResponseDTO(
        long completedMissionCount,
        int totalPoint,
        long acquiredBadgeCount,
        long totalBadgeCount,
        long acquiredStampCount,
        long totalStampCount
) {
}
