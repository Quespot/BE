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
import com.quespot.domain.mission.dto.res.UnlockConditionResponseDTO;
import com.quespot.domain.mission.dto.res.VerificationGuideResponseDTO;
import com.quespot.domain.mission.entity.MissionAttempt;
import com.quespot.domain.mission.exception.code.MissionSuccessCode;
import com.quespot.domain.mission.service.CourseLockPolicy;
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

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "MissionAttempt", description = "미션 수행 API")
public class MissionAttemptController {

    private final MissionAttemptService missionAttemptService;
    private final MissionArrivalService missionArrivalService;
    private final MissionPhotoService missionPhotoService;
    private final CourseLockPolicy courseLockPolicy;

    @PostMapping("/api/missions/{missionId}/start")
    @Operation(summary = "미션 시작")
    public ApiResponse<MissionAttemptResponseDTO> start(
            @PathVariable @Positive Long missionId,
            @RequestParam(required = false) @Positive Long courseAttemptId,
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
        String photoViewUrl = missionPhotoService.resolveViewUrl(photo);
        return ApiResponse.of(
                MissionSuccessCode.MISSION_ATTEMPT_RESULT_FOUND,
                MissionConverter.toAttemptResultResponse(attempt, photoViewUrl)
        );
    }

    @PostMapping("/api/mission-attempts/{attemptId}/photos")
    @Operation(
            summary = "완료 후 사진 기록",
            description = """
                    업로드 3단계 흐름의 마지막 단계. 먼저 POST /api/files/presigned-upload-url(purpose=MISSION)로
                    objectKey와 uploadUrl을 발급받아 파일을 직접 PUT한 뒤, 그 objectKey를 그대로 제출한다
                    (버킷이 비공개라 URL이 아니라 objectKey를 제출한다 — 조회 시 서버가 매번 새로 서명한
                    presigned GET URL로 응답한다). missions/ 아래이고 본인이 발급받은 objectKey가 아니면
                    거부된다.
                    """
    )
    public ApiResponse<MissionPhotoResponseDTO> registerPhoto(
            @PathVariable @Positive Long attemptId,
            @Valid @RequestBody RegisterMissionPhotoRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        var photo = missionPhotoService.registerPhoto(
                principal.userId(), attemptId, request.objectKey(), request.caption(),
                request.latitude(), request.longitude(), request.takenAt()
        );
        String photoViewUrl = missionPhotoService.resolveViewUrl(photo);
        return ApiResponse.of(MissionSuccessCode.MISSION_PHOTO_REGISTERED, MissionConverter.toPhotoResponse(photo, photoViewUrl));
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

    @GetMapping("/api/missions/{missionId}/unlock-condition")
    @Operation(summary = "코스 잠금 조건 조회")
    public ApiResponse<UnlockConditionResponseDTO> getUnlockCondition(
            @PathVariable @Positive Long missionId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        boolean locked = courseLockPolicy
                .resolveLockedMissionIds(principal.userId(), List.of(missionId))
                .contains(missionId);
        return ApiResponse.of(MissionSuccessCode.UNLOCK_CONDITION_FOUND, MissionConverter.toUnlockConditionResponse(locked));
    }
}
