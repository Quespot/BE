package com.quespot.domain.reward.controller;

import com.quespot.domain.reward.dto.res.PointResponseDTO;
import com.quespot.domain.reward.exception.code.RewardSuccessCode;
import com.quespot.domain.reward.service.PointService;
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
@RequestMapping("/api/users/me/points")
@Tag(name = "Reward", description = "보상 API")
public class PointController {

    private final PointService pointService;

    @GetMapping
    @Operation(
            summary = "보유 포인트 조회",
            description = "로그인한 사용자의 보유 포인트, 누적 획득/사용 포인트를 조회한다. 적립 이력이 없으면 0을 반환한다."
    )
    public ApiResponse<PointResponseDTO> getPoints(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(RewardSuccessCode.POINTS_FOUND, pointService.getPoints(principal.userId()));
    }
}
