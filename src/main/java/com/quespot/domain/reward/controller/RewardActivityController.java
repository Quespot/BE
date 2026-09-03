package com.quespot.domain.reward.controller;

import com.quespot.domain.reward.dto.res.RewardActivityListResponseDTO;
import com.quespot.domain.reward.exception.code.RewardSuccessCode;
import com.quespot.domain.reward.service.RewardActivityService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/reward-activities")
@Tag(name = "Reward", description = "보상 API")
@Validated
public class RewardActivityController {

    private final RewardActivityService rewardActivityService;

    @GetMapping
    @Operation(
            summary = "보상 활동 내역 조회",
            description = "로그인한 사용자의 포인트 적립/사용, 배지·스탬프 획득 내역을 최신순 커서 페이지네이션으로 조회한다."
    )
    public ApiResponse<RewardActivityListResponseDTO> getActivities(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Positive @Max(100) int size
    ) {
        return ApiResponse.of(
                RewardSuccessCode.REWARD_ACTIVITIES_FOUND,
                rewardActivityService.getActivities(principal.userId(), cursor, size)
        );
    }
}
