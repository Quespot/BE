package com.quespot.domain.mission.controller;

import com.quespot.domain.mission.converter.MissionConverter;
import com.quespot.domain.mission.dto.req.MissionCourseGenerateRequestDTO;
import com.quespot.domain.mission.dto.res.CourseAttemptListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionCourseDetailResponseDTO;
import com.quespot.domain.mission.dto.res.MissionCourseListItemResponseDTO;
import com.quespot.domain.mission.exception.code.MissionSuccessCode;
import com.quespot.domain.mission.service.CourseAttemptService;
import com.quespot.domain.mission.service.MissionCourseGenerationService;
import com.quespot.domain.mission.service.MissionCourseQueryService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "MissionCourse", description = "미션 코스 API")
public class MissionCourseController {

    private final MissionCourseQueryService missionCourseQueryService;
    private final MissionCourseGenerationService missionCourseGenerationService;
    private final CourseAttemptService courseAttemptService;

    @GetMapping("/api/mission-courses")
    @Operation(
            summary = "내가 만든 코스 목록 조회",
            description = "커서 페이징 없이 전체를 최신순으로 돌려준다. 없으면 빈 배열. 각 항목의 myStatus는 시작한 적 없으면 null이다."
    )
    public ApiResponse<List<MissionCourseListItemResponseDTO>> getCourses(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        var courses = missionCourseQueryService.getCourses(principal.userId()).stream()
                .map(MissionConverter::toCourseListItem)
                .toList();
        return ApiResponse.of(MissionSuccessCode.COURSES_FOUND, courses);
    }

    @PostMapping("/api/mission-courses")
    @Operation(
            summary = "미션 코스 생성(즉시 시작)",
            description = """
                    **생성과 시작이 한 번에 일어난다**(별도 start API 없음). anchorMissionId가 seq 1로 고정되고 주변에서 조건에 맞는
                    미션 2개를 자동으로 붙여 3곳짜리 코스가 된다. 같은 anchor로 다시 만들면 다른 조합이 나올 수 있다.

                    응답은 코스 상세와 같은 형태다. missions 배열은 seq 순이며 seq 1만 AVAILABLE, 나머지는 LOCKED로 시작한다.
                    각 미션은 POST /api/missions/{missionId}/start?courseAttemptId=... 로 시작하고, 앞 미션을 완료하면 다음 하나만
                    AVAILABLE로 바뀐다. myStatus는 생성 직후라 항상 IN_PROGRESS다.

                    anchor는 아직 시작·완료하지 않은 미션이어야 하고(409), 주변에 이어갈 미션이 부족하면 생성에 실패한다(409).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "MISSION_404_003 anchor 미션 없음·비활성",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "MISSION_409_009 이미 시작·완료한 미션은 anchor 불가 — 다른 미션 선택 유도 / MISSION_409_010 주변 미션 부족 — \"이 근처엔 코스를 만들 수 없어요\" 안내",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<MissionCourseDetailResponseDTO> generateCourse(
            @Valid @RequestBody MissionCourseGenerateRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        var result = missionCourseGenerationService.generateAndStart(principal.userId(), request.anchorMissionId());
        return ApiResponse.of(MissionSuccessCode.COURSE_GENERATED, MissionConverter.toCourseDetail(result));
    }

    @GetMapping("/api/mission-courses/{courseId}")
    @Operation(
            summary = "미션 코스 상세 조회",
            description = """
                    missions 배열(seq 순)의 status 의미:
                    - LOCKED — 앞 순서 미션이 완료되지 않음. 시작할 수 없다(미션 목록에서 직접 start해도 MISSION_409_011로 막힌다)
                    - AVAILABLE — 지금 시작할 수 있다
                    - IN_PROGRESS — 진행 중
                    - COMPLETED — 완료

                    앞 미션을 완료하면 **다음 하나만** AVAILABLE로 바뀌므로, 미션 완료 후 이 API를 다시 조회해 화면을 갱신한다.
                    myStatus는 내 코스 진행 상태이며 시작한 적 없으면 null이다. 코스는 비공개라 본인이 만든 코스만 조회할 수 있고, 아니면 404다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "MISSION_404_005 코스 없음·비활성 또는 남의 코스 — 코스 목록 새로고침",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<MissionCourseDetailResponseDTO> getCourseDetail(
            @PathVariable @Positive Long courseId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        var result = missionCourseQueryService.getCourseDetail(principal.userId(), courseId);
        return ApiResponse.of(MissionSuccessCode.COURSE_FOUND, MissionConverter.toCourseDetail(result));
    }

    @GetMapping("/api/course-attempts")
    @Operation(
            summary = "진행 중인 코스 조회",
            description = "앱 재시작 후 진행 중 코스를 복구할 때 쓴다. 코스별로 IN_PROGRESS 시도는 최대 1건이다."
    )
    public ApiResponse<CourseAttemptListResponseDTO> getInProgressCourseAttempts(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        var attempts = courseAttemptService.getInProgressList(principal.userId());
        return ApiResponse.of(
                MissionSuccessCode.COURSE_ATTEMPTS_FOUND,
                MissionConverter.toCourseAttemptListResponse(attempts)
        );
    }

    @PostMapping("/api/course-attempts/{courseAttemptId}/quit")
    @Operation(
            summary = "미션 코스 포기",
            description = "코스만 포기한다. 그 코스에서 시작한 개별 미션 시도는 포기되지 않고 단독 수행으로 이어진다. 완주한 코스는 다시 시작할 수 없다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "MISSION_404_006 코스 진행을 찾을 수 없음 — 코스 화면으로",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "MISSION_409_008 진행 중인 코스가 아님(이미 완주·포기됨) — 상태 재조회",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<Void> quitCourse(
            @PathVariable @Positive Long courseAttemptId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        courseAttemptService.quit(principal.userId(), courseAttemptId);
        return ApiResponse.<Void>of(MissionSuccessCode.COURSE_ATTEMPT_QUIT, null);
    }
}
