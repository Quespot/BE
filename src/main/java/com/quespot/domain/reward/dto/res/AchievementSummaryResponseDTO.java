package com.quespot.domain.reward.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
// 홈 상단 4개 숫자와 마이페이지 달성 현황이 같이 쓴다. 분모(totalBadgeCount=
// is_active 배지 수, totalStampCount=전체 8개 슬롯)는 서버가 센다 — 클라이언트
// 하드코딩(6/8 불일치) 방지. 완료 미션 분모(2/20의 20)는 미정이라 내리지 않는다.
public record AchievementSummaryResponseDTO(
        @Schema(description = "완료 미션 수. 분모는 기획 미확정이라 내려주지 않는다")
        long completedMissionCount,
        @Schema(description = "현재 잔액")
        int totalPoint,
        long acquiredBadgeCount,
        @Schema(description = "활성 배지 수. 서버가 계산하므로 하드코딩하지 말 것")
        long totalBadgeCount,
        long acquiredStampCount,
        @Schema(description = "전체 스탬프 슬롯 수(잠금 포함). 서버가 계산하므로 하드코딩하지 말 것")
        long totalStampCount
) {
}
