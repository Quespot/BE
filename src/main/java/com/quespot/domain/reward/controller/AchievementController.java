package com.quespot.domain.reward.controller;

import com.quespot.domain.reward.dto.res.AchievementSummaryResponseDTO;
import com.quespot.domain.reward.exception.code.RewardSuccessCode;
import com.quespot.domain.reward.service.AchievementQueryService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/achievements")
@Tag(name = "Reward", description = "보상 API")
public class AchievementController {

    private final AchievementQueryService achievementQueryService;

    @GetMapping
    @Operation(
            summary = "달성 현황 요약",
            description = """
                    홈 상단 숫자와 마이페이지 달성 현황이 같이 쓴다. 완료 미션 수, 보유 포인트(잔액), 배지·스탬프 획득 수와 전체 수를
                    한 번에 돌려준다. **분모(totalBadgeCount, totalStampCount)는 서버가 계산해 내리므로 하드코딩하지 말 것** —
                    배지는 활성 마스터 수, 스탬프는 잠금 포함 전체 슬롯 수다. completedMissionCount는 분모가 없다(기준 미정).
                    """
    )
    public ApiResponse<AchievementSummaryResponseDTO> getSummary(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(RewardSuccessCode.ACHIEVEMENTS_FOUND, achievementQueryService.getSummary(principal.userId()));
    }
}
