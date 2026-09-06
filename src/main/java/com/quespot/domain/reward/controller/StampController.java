package com.quespot.domain.reward.controller;

import com.quespot.domain.reward.dto.res.StampListResponseDTO;
import com.quespot.domain.reward.exception.code.RewardSuccessCode;
import com.quespot.domain.reward.service.StampService;
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
@RequestMapping("/api/users/me/stamps")
@Tag(name = "Reward", description = "보상 API")
public class StampController {

    private final StampService stampService;

    @GetMapping
    @Operation(
            summary = "스탬프 목록 조회",
            description = "전체 스탬프 마스터 목록에 로그인한 사용자의 획득 여부와 수집 가능 여부를 함께 조회한다."
    )
    public ApiResponse<StampListResponseDTO> getStamps(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(RewardSuccessCode.STAMPS_FOUND, stampService.getStamps(principal.userId()));
    }
}
