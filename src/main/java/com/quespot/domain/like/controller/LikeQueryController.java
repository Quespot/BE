package com.quespot.domain.like.controller;

import com.quespot.domain.like.dto.res.LikedCourseListResponseDTO;
import com.quespot.domain.like.dto.res.LikedMissionListResponseDTO;
import com.quespot.domain.like.exception.code.LikeSuccessCode;
import com.quespot.domain.like.service.LikeQueryService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Like", description = "좋아요 API")
public class LikeQueryController {

    private final LikeQueryService likeQueryService;

    @GetMapping("/api/users/me/liked-missions")
    @Operation(
            summary = "좋아요한 미션 목록",
            description = "좋아요한 시각 내림차순. 비활성 미션은 제외되며 totalCount는 노출 건수다. "
                    + "거리·평점은 내려주지 않는다."
    )
    public ApiResponse<LikedMissionListResponseDTO> getLikedMissions(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(LikeSuccessCode.LIKED_MISSIONS_FOUND, likeQueryService.getLikedMissions(principal.userId()));
    }

    @GetMapping("/api/users/me/liked-courses")
    @Operation(
            summary = "좋아요한 코스 목록",
            description = "좋아요한 시각 내림차순. 코스 목록과 같은 카드 형태(지역·미션 수·보상·내 진행 상태)로 내려준다."
    )
    public ApiResponse<LikedCourseListResponseDTO> getLikedCourses(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(LikeSuccessCode.LIKED_COURSES_FOUND, likeQueryService.getLikedCourses(principal.userId()));
    }
}
