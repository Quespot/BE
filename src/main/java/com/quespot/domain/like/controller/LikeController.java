package com.quespot.domain.like.controller;

import com.quespot.domain.like.enums.LikeTargetType;
import com.quespot.domain.like.exception.code.LikeSuccessCode;
import com.quespot.domain.like.service.LikeService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

// 리소스별 prefix가 둘(missions/mission-courses)이라 클래스 레벨 @RequestMapping
// 없이 메서드에 전체 경로를 쓴다(MissionAttemptController 방식).
// 토글 엔드포인트는 만들지 않는다 — 재시도 시 상태가 뒤집힌다(CLAUDE.md).
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "Like", description = "좋아요 API")
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/api/missions/{missionId}/like")
    @Operation(summary = "미션 좋아요", description = "이미 좋아요 상태여도 200(멱등, 토글 아님). 잠긴 미션은 409, 비활성 미션은 404.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "MISSION_404_003 미션 없음·비활성 — 목록 새로고침",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "MISSION_409_011 잠긴 미션 — 하트 비활성 처리",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<Void> likeMission(
            @PathVariable @Positive Long missionId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        likeService.likeMission(principal.userId(), missionId);
        return ApiResponse.<Void>of(LikeSuccessCode.LIKED, null);
    }

    @DeleteMapping("/api/missions/{missionId}/like")
    @Operation(summary = "미션 좋아요 해제", description = "좋아요가 없어도 200.")
    public ApiResponse<Void> unlikeMission(
            @PathVariable @Positive Long missionId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        likeService.unlike(principal.userId(), LikeTargetType.MISSION, missionId);
        return ApiResponse.<Void>of(LikeSuccessCode.UNLIKED, null);
    }

    @PostMapping("/api/mission-courses/{courseId}/like")
    @Operation(summary = "코스 좋아요", description = "본인이 생성한 ACTIVE 코스만. 아니면 404. 이미 좋아요 상태여도 200(멱등, 토글 아님).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "MISSION_404_005 코스 없음·비활성 또는 남의 코스",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<Void> likeCourse(
            @PathVariable @Positive Long courseId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        likeService.likeCourse(principal.userId(), courseId);
        return ApiResponse.<Void>of(LikeSuccessCode.LIKED, null);
    }

    @DeleteMapping("/api/mission-courses/{courseId}/like")
    @Operation(summary = "코스 좋아요 해제", description = "좋아요가 없어도 200.")
    public ApiResponse<Void> unlikeCourse(
            @PathVariable @Positive Long courseId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        likeService.unlike(principal.userId(), LikeTargetType.COURSE, courseId);
        return ApiResponse.<Void>of(LikeSuccessCode.UNLIKED, null);
    }
}
