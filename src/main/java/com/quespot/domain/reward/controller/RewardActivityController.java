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
            description = """
                    포인트 적립/사용, 배지·스탬프 획득 내역을 최신순으로 돌려준다. 커서 페이징: 첫 요청은 cursor 없이,
                    응답의 nextCursor(숫자 id)를 다음 요청의 cursor에 그대로 넣는다. hasNext=false면 마지막 페이지이고
                    nextCursor는 null. 결과가 없으면 activities는 빈 배열이다.
                    amount는 포인트 변동량(적립 +, 사용 -)이며 배지·스탬프 획득처럼 포인트 변동이 없는 활동이면 null이다.
                    """
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
