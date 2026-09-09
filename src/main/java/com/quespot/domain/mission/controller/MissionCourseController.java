package com.quespot.domain.mission.controller;

import com.quespot.domain.mission.converter.MissionConverter;
import com.quespot.domain.mission.dto.res.CourseAttemptListResponseDTO;
import com.quespot.domain.mission.dto.res.CourseAttemptResponseDTO;
import com.quespot.domain.mission.dto.res.MissionCourseDetailResponseDTO;
import com.quespot.domain.mission.dto.res.MissionCourseListItemResponseDTO;
import com.quespot.domain.mission.entity.CourseAttempt;
import com.quespot.domain.mission.exception.code.MissionSuccessCode;
import com.quespot.domain.mission.service.CourseAttemptService;
import com.quespot.domain.mission.service.MissionCourseQueryService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "MissionCourse", description = "미션 코스 API")
public class MissionCourseController {

    private final MissionCourseQueryService missionCourseQueryService;
    private final CourseAttemptService courseAttemptService;

    @GetMapping("/api/mission-courses")
    @Operation(summary = "미션 코스 목록 조회")
    public ApiResponse<List<MissionCourseListItemResponseDTO>> getCourses(
            @RequestParam(required = false) String regionCode
    ) {
        var courses = missionCourseQueryService.getCourses(regionCode).stream()
                .map(MissionConverter::toCourseListItem)
                .toList();
        return ApiResponse.of(MissionSuccessCode.COURSES_FOUND, courses);
    }

    @GetMapping("/api/mission-courses/{courseId}")
    @Operation(summary = "미션 코스 상세 조회")
    public ApiResponse<MissionCourseDetailResponseDTO> getCourseDetail(
            @PathVariable @Positive Long courseId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        var result = missionCourseQueryService.getCourseDetail(principal.userId(), courseId);
        return ApiResponse.of(MissionSuccessCode.COURSE_FOUND, MissionConverter.toCourseDetail(result));
    }

    @PostMapping("/api/mission-courses/{courseId}/start")
    @Operation(summary = "미션 코스 시작")
    public ApiResponse<CourseAttemptResponseDTO> startCourse(
            @PathVariable @Positive Long courseId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        CourseAttempt attempt = courseAttemptService.start(principal.userId(), courseId);
        return ApiResponse.of(MissionSuccessCode.COURSE_ATTEMPT_STARTED, MissionConverter.toCourseAttemptResponse(attempt));
    }

    @GetMapping("/api/course-attempts")
    @Operation(summary = "진행 중인 코스 조회")
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
    @Operation(summary = "미션 코스 포기")
    public ApiResponse<Void> quitCourse(
            @PathVariable @Positive Long courseAttemptId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        courseAttemptService.quit(principal.userId(), courseAttemptId);
        return ApiResponse.<Void>of(MissionSuccessCode.COURSE_ATTEMPT_QUIT, null);
    }
}
