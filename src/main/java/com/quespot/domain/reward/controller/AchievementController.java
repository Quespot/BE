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
            description = "완료 미션 수, 보유 포인트, 배지/스탬프 획득 수와 전체 수를 한 번에 조회한다. "
                    + "분모는 서버가 계산한다(배지=활성 마스터 수, 스탬프=전체 슬롯 수). "
                    + "완료 미션 분모는 기획 미확정이라 내리지 않는다."
    )
    public ApiResponse<AchievementSummaryResponseDTO> getSummary(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(RewardSuccessCode.ACHIEVEMENTS_FOUND, achievementQueryService.getSummary(principal.userId()));
    }
}
