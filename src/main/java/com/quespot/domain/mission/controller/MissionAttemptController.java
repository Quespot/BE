package com.quespot.domain.mission.controller;

import com.quespot.domain.mission.converter.MissionConverter;
import com.quespot.domain.mission.dto.ArrivalResultDTO;
import com.quespot.domain.mission.dto.req.ArrivalRequestDTO;
import com.quespot.domain.mission.dto.req.RegisterMissionPhotoRequestDTO;
import com.quespot.domain.mission.dto.req.ReflectionRequestDTO;
import com.quespot.domain.mission.dto.res.ArrivalResponseDTO;
import com.quespot.domain.mission.dto.res.MissionAttemptListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionAttemptResponseDTO;
import com.quespot.domain.mission.dto.res.MissionAttemptResultResponseDTO;
import com.quespot.domain.mission.dto.res.MissionPhotoResponseDTO;
import com.quespot.domain.mission.dto.res.VerificationGuideResponseDTO;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.exception.code.MissionSuccessCode;
import com.quespot.domain.mission.service.MissionArrivalService;
import com.quespot.domain.mission.service.MissionAttemptService;
import com.quespot.domain.mission.service.MissionPhotoService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "MissionAttempt", description = "미션 수행 API")
public class MissionAttemptController {

    private final MissionAttemptService missionAttemptService;
    private final MissionArrivalService missionArrivalService;
    private final MissionPhotoService missionPhotoService;

    @PostMapping("/api/missions/{missionId}/start")
    @Operation(summary = "미션 시작")
    public ApiResponse<MissionAttemptResponseDTO> start(
            @PathVariable @Positive Long missionId,
            @RequestParam(required = false) Long courseAttemptId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        MissionAttempt attempt = missionAttemptService.start(principal.userId(), missionId, courseAttemptId);
        return ApiResponse.of(MissionSuccessCode.MISSION_ATTEMPT_STARTED, MissionConverter.toAttemptResponse(attempt));
    }

    @GetMapping("/api/mission-attempts")
    @Operation(summary = "진행 중인 미션 시도 목록 조회")
    public ApiResponse<MissionAttemptListResponseDTO> getInProgressAttempts(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(
                MissionSuccessCode.MISSION_ATTEMPTS_FOUND,
                MissionConverter.toAttemptListResponse(missionAttemptService.getInProgressList(principal.userId()))
        );
    }

    @GetMapping("/api/mission-attempts/{attemptId}")
    @Operation(summary = "미션 시도 단건 조회")
    public ApiResponse<MissionAttemptResponseDTO> getAttempt(
            @PathVariable @Positive Long attemptId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        MissionAttempt attempt = missionAttemptService.getAttempt(principal.userId(), attemptId);
        return ApiResponse.of(MissionSuccessCode.MISSION_ATTEMPT_FOUND, MissionConverter.toAttemptResponse(attempt));
    }

    @PostMapping("/api/mission-attempts/{attemptId}/arrival")
    @Operation(summary = "GPS 도착 인증")
    public ApiResponse<ArrivalResponseDTO> arrive(
            @PathVariable @Positive Long attemptId,
            @Valid @RequestBody ArrivalRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        ArrivalResultDTO result = missionArrivalService.arrive(
                principal.userId(), attemptId, request.latitude(), request.longitude()
        );
        return ApiResponse.of(MissionSuccessCode.MISSION_ATTEMPT_ARRIVED, MissionConverter.toArrivalResponse(result));
    }

    @GetMapping("/api/mission-attempts/{attemptId}/verification-guide")
    @Operation(summary = "인증 가이드 조회")
    public ApiResponse<VerificationGuideResponseDTO> getVerificationGuide(
            @PathVariable @Positive Long attemptId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        MissionAttempt attempt = missionAttemptService.getAttempt(principal.userId(), attemptId);
        return ApiResponse.of(
                MissionSuccessCode.MISSION_ATTEMPT_GUIDE_FOUND,
                MissionConverter.toVerificationGuideResponse(attempt)
        );
    }

    @GetMapping("/api/mission-attempts/{attemptId}/result")
    @Operation(summary = "완료 결과 조회")
    public ApiResponse<MissionAttemptResultResponseDTO> getResult(
            @PathVariable @Positive Long attemptId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        MissionAttempt attempt = missionAttemptService.getAttempt(principal.userId(), attemptId);
        var photo = missionPhotoService.findByAttemptId(attemptId).orElse(null);
        return ApiResponse.of(
                MissionSuccessCode.MISSION_ATTEMPT_RESULT_FOUND,
                MissionConverter.toAttemptResultResponse(attempt, photo)
        );
    }

    @PostMapping("/api/mission-attempts/{attemptId}/photos")
    @Operation(summary = "완료 후 사진 기록")
    public ApiResponse<MissionPhotoResponseDTO> registerPhoto(
            @PathVariable @Positive Long attemptId,
            @Valid @RequestBody RegisterMissionPhotoRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        var photo = missionPhotoService.registerPhoto(
                principal.userId(), attemptId, request.imageUrl(), request.caption(),
                request.latitude(), request.longitude(), request.takenAt()
        );
        return ApiResponse.of(MissionSuccessCode.MISSION_PHOTO_REGISTERED, MissionConverter.toPhotoResponse(photo));
    }

    @PostMapping("/api/mission-attempts/{attemptId}/reflection")
    @Operation(summary = "감상 기록")
    public ApiResponse<Void> writeReflection(
            @PathVariable @Positive Long attemptId,
            @Valid @RequestBody ReflectionRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        missionAttemptService.writeReflection(principal.userId(), attemptId, request.content());
        return ApiResponse.<Void>of(MissionSuccessCode.MISSION_REFLECTION_REGISTERED, null);
    }

    @PostMapping("/api/mission-attempts/{attemptId}/quit")
    @Operation(summary = "미션 시도 포기")
    public ApiResponse<Void> quit(
            @PathVariable @Positive Long attemptId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        missionAttemptService.quit(principal.userId(), attemptId);
        return ApiResponse.<Void>of(MissionSuccessCode.MISSION_ATTEMPT_QUIT, null);
    }
}
