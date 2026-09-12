package com.quespot.domain.mission.controller;

import com.quespot.domain.mission.dto.res.MissionDetailResponseDTO;
import com.quespot.domain.mission.dto.res.MissionListResponseDTO;
import com.quespot.domain.mission.dto.res.RecommendedMissionListResponseDTO;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.exception.code.MissionSuccessCode;
import com.quespot.domain.mission.service.MissionQueryService;
import com.quespot.domain.mission.service.MissionRecommendationService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/missions")
@Tag(name = "MissionDiscovery", description = "미션 탐색 API")
public class MissionDiscoveryController {

    private final MissionQueryService missionQueryService;
    private final MissionRecommendationService missionRecommendationService;

    @GetMapping
    @Operation(
            summary = "미션 목록 조회",
            description = """
                    커서 페이징. 첫 요청은 cursor 없이 보내고, 응답의 nextCursor를 다음 요청의 cursor에 **그대로** 넣는다.
                    hasNext=false면 마지막 페이지이고 그때 nextCursor는 null이다. 결과가 없으면 missions는 빈 배열이다(null 아님).
                    cursor는 서명된 값이라 임의로 만들거나 파싱하지 말 것 — 형식이 바뀔 수 있다.

                    **커서에는 조회 조건이 묶여 있다**: category, keyword, latitude/longitude(유무 포함), 그리고 정렬 모드.
                    좌표를 보내면 거리순(DISTANCE), 안 보내면 사용자별 랜덤순(RANDOM)이다. 이 중 하나라도 바꾸면
                    커서를 버리고 cursor 없이 다시 요청해야 한다. 안 맞는 커서는 MISSION_400_002로 거부된다.

                    **랜덤순 커서는 하루짜리다.** 랜덤 순서의 시드가 날짜(KST)로 정해지므로 자정을 넘기면 이전 커서가
                    MISSION_400_002로 거부된다. 날짜가 바뀌면 커서를 버리고 cursor 없이 처음부터 다시 요청한다.

                    userMissionStatus=LOCKED는 진행 중인 코스에서 앞 미션이 아직 완료되지 않았다는 뜻이다(canStart=false).
                    """
    )
    public ApiResponse<MissionListResponseDTO> getMissions(
            @RequestParam(required = false) MissionCategory category,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) @Size(max = 500) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(
                MissionSuccessCode.MISSIONS_FOUND,
                missionQueryService.getMissions(
                        principal.userId(),
                        category,
                        keyword,
                        latitude,
                        longitude,
                        cursor,
                        size
                )
        );
    }

    @GetMapping("/{missionId}")
    @Operation(
            summary = "미션 상세 조회",
            description = """
                    userMissionStatus에 따라 버튼을 결정한다:
                    - AVAILABLE — canStart=true. 시작 버튼
                    - IN_PROGRESS — 진행 중 시도로 이동(GET /api/mission-attempts에서 attemptId 확인)
                    - COMPLETED — 완료 표시. canCreateArchive=true라 사진 등록 가능
                    - LOCKED — 시작 불가. GET /api/missions/{missionId}/unlock-condition의 message로 이유를 보여준다

                    비활성 미션은 상태가 아니라 404(MISSION_404_003)다. distanceMeters는 latitude/longitude를 보냈을 때만
                    채워진다. liked는 현재 미구현이라 항상 false다 — 좋아요 여부는 GET /api/users/me/liked-missions로 판단한다.
                    """
    )
    public ApiResponse<MissionDetailResponseDTO> getMission(
            @PathVariable @Positive Long missionId,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(
                MissionSuccessCode.MISSION_FOUND,
                missionQueryService.getMission(
                        principal.userId(),
                        missionId,
                        latitude,
                        longitude
                )
        );
    }

    @GetMapping("/recommendations")
    @Operation(
            summary = "추천 미션 조회",
            description = """
                    사용자의 여행 스타일과 현재 위치를 기준으로 바로 시작할 수 있는 미션만 추천한다. 홈은 size=2,
                    전체보기는 size와 cursor를 사용한다. latitude/longitude는 함께 보내거나 모두 생략해야 하며, 좌표를
                    보내면 카테고리별 가까운 미션을 우선하고 생략하면 일일 고정 랜덤순이다. 선호 카테고리를 우선하면서
                    가능한 경우 카테고리를 번갈아 배치하고, 후보가 부족하면 다른 카테고리로 보충한다.

                    첫 요청은 cursor 없이 보내고 응답의 nextCursor를 다음 요청에 그대로 넣는다. 사용자 여행 스타일,
                    좌표 또는 날짜(KST)가 바뀌면 기존 커서는 MISSION_400_002로 거부되므로 처음부터 다시 조회한다.
                    완료·진행 중·코스에서 잠긴 미션은 제외되며, 결과가 없으면 missions는 빈 배열이다.
                    """
    )
    public ApiResponse<RecommendedMissionListResponseDTO> getRecommendedMissions(
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) @Size(max = 500) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(
                MissionSuccessCode.RECOMMENDED_MISSIONS_FOUND,
                missionRecommendationService.getRecommendations(
                        principal.userId(), latitude, longitude, cursor, size
                )
        );
    }
}
