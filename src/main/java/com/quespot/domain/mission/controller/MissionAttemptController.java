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
    @Operation(
            summary = "미션 시작",
            description = """
                    미션 수행 흐름의 1단계. 응답의 attemptId를 이후 arrival / photos / result / quit에서 쓴다.
                    코스 진행 중에 시작하면 courseAttemptId를 쿼리로 함께 보낸다(코스에 속하지 않는 미션이면 400).
                    앱을 껐다 켜면 GET /api/mission-attempts로 진행 중 시도를 복구한다.
                    같은 미션의 진행 중 시도가 이미 있으면 새로 만들지 않고 그 시도를 돌려준다(응답 status는 항상 IN_PROGRESS).
                    이미 완료한 미션은 재도전할 수 없고(409), 코스에서 앞 미션을 완료하지 않은 잠긴 미션도 시작할 수 없다(409).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "MISSION_400_003 해당 코스에 속하지 않는 미션 — courseAttemptId를 빼거나 코스 상세의 missions에서 고른다",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "MISSION_404_003 미션 없음·비활성 — 목록 새로고침 / MISSION_404_006 코스 진행을 찾을 수 없음 — 코스 화면으로",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "MISSION_409_011 잠긴 미션 — 이전 미션을 먼저 완료하라고 안내 / MISSION_409_002 이미 완료한 미션 — 재도전 불가 안내 / MISSION_409_008 진행 중인 코스가 아님 — 코스 없이 단독 시작 유도",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<MissionAttemptResponseDTO> start(
            @PathVariable @Positive Long missionId,
            @RequestParam(required = false) @Positive Long courseAttemptId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        MissionAttempt attempt = missionAttemptService.start(principal.userId(), missionId, courseAttemptId);
        return ApiResponse.of(MissionSuccessCode.MISSION_ATTEMPT_STARTED, MissionConverter.toAttemptResponse(attempt));
    }

    @GetMapping("/api/mission-attempts")
    @Operation(
            summary = "진행 중인 미션 시도 목록 조회",
            description = "앱 재시작 후 진행 중 미션을 복구할 때 쓴다. IN_PROGRESS 상태만 반환하며, 미션당 최대 1건이다."
    )
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
    @Operation(
            summary = "GPS 도착 인증",
            description = """
                    미션 수행 흐름의 2단계. 현재 위치를 보내면 GPS 판정 후 **즉시** 결과를 돌려준다(비동기 아님).

                    **인증 실패도 HTTP 200이다.** 4xx가 아니라 `success:false`로 온다. 이때 `distanceMeters`와 `radiusMeters`를
                    함께 주므로 "1,072m 떨어져 있어요. 500m 안으로 가주세요" 같은 안내를 만들 수 있다. 실패해도 시도 상태는
                    바뀌지 않으므로(IN_PROGRESS 유지) 위치를 다시 잡아 같은 API를 재시도하면 된다.

                    성공(`success:true`)하면 그 자리에서 `status`가 COMPLETED가 되고 `earnedPoint`가 채워지며 포인트 지급과
                    배지·스탬프 판정까지 끝난다. 이미 완료된 시도에 다시 보내면 멱등하게 `success:true`를 돌려준다(포인트 재지급 없음).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "MISSION_404_004 시도를 찾을 수 없음(타인의 시도 포함) — 진행 중 목록을 다시 조회",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "MISSION_409_004 포기한 시도 — 미션을 새로 시작하라고 안내",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
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
    @Operation(
            summary = "완료 결과 조회",
            description = "완료 화면용. 획득 포인트와 함께, 사진을 등록했다면 매번 새로 서명한 presigned 조회 URL을 준다."
    )
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
                    미션 수행 흐름의 3단계(선택). 사진이 없어도 미션은 이미 완료된 상태다. 미션당 1장이며 완료 후 언제든 등록할 수 있다.
                    등록한 사진은 GET /api/users/me/archives 피드에 source=MISSION으로 나타난다.

                    업로드는 3단계다: ① POST /api/files/presigned-upload-url(purpose=MISSION)로 objectKey와 uploadUrl 발급 →
                    ② uploadUrl로 파일을 직접 PUT → ③ 그 objectKey를 그대로 여기 제출(버킷이 비공개라 URL이 아니라 objectKey를 낸다.
                    조회 시 서버가 매번 새로 서명한 presigned GET URL로 응답한다). missions/ 아래이고 본인이 발급받은 objectKey가 아니면 거부된다.
                    위도·경도는 둘 다 보내거나 둘 다 비운다(비우면 미션 스냅샷 좌표로 저장).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "MISSION_400_001 위도·경도 한쪽만 옴 / FILE_400_004 objectKey 형식 오류 / FILE_400_005 missions/ 아래가 아님(purpose 불일치) / FILE_400_006 본인이 업로드한 파일이 아님 — FILE_* 셋은 업로드 1단계부터 다시",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "MISSION_404_004 시도를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "MISSION_409_006 완료 전 — 도착 인증을 먼저 / MISSION_409_005 이미 사진 있음 — 등록 버튼 숨김",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
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
    @Operation(
            summary = "감상 기록",
            description = "진행 중·완료 어느 상태에서든 감상을 남길 수 있다. 다시 보내면 덮어쓴다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "MISSION_404_004 시도를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<Void> writeReflection(
            @PathVariable @Positive Long attemptId,
            @Valid @RequestBody ReflectionRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        missionAttemptService.writeReflection(principal.userId(), attemptId, request.content());
        return ApiResponse.<Void>of(MissionSuccessCode.MISSION_REFLECTION_REGISTERED, null);
    }

    @PostMapping("/api/mission-attempts/{attemptId}/quit")
    @Operation(
            summary = "미션 시도 포기",
            description = "진행 중인 시도를 포기한다. 완료된 시도는 포기할 수 없다. 코스 소속 미션을 포기해도 코스 자체는 계속 진행 중이다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "MISSION_404_004 시도를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "MISSION_409_003 진행 중인 시도가 아님(이미 완료·포기됨) — 상태 재조회",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ApiResponse<Void> quit(
            @PathVariable @Positive Long attemptId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        missionAttemptService.quit(principal.userId(), attemptId);
        return ApiResponse.<Void>of(MissionSuccessCode.MISSION_ATTEMPT_QUIT, null);
    }

    @GetMapping("/api/missions/{missionId}/unlock-condition")
    @Operation(
            summary = "코스 잠금 조건 조회",
            description = """
                    미션 시작 버튼을 그리기 전에 잠김 여부를 확인한다. 진행 중인 코스에서 앞 순서(seq) 미션을 아직 완료하지
                    않았으면 locked=true. 코스와 무관한 미션은 항상 locked=false. 시작 API가 같은 규칙으로 MISSION_409_011을 낸다.
                    """
    )
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
