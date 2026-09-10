package com.quespot.domain.mission.controller;

import com.quespot.domain.mission.dto.res.MissionListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionSpotMapResponseDTO;
import com.quespot.domain.mission.dto.res.NearbyMissionSpotListResponseDTO;
import com.quespot.domain.mission.exception.code.MissionSuccessCode;
import com.quespot.domain.mission.service.MissionSpotQueryService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
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
@RequestMapping("/api/mission-spots")
@Tag(name = "MissionSpot", description = "미션 스팟 API")
public class MissionSpotController {

    private final MissionSpotQueryService missionSpotQueryService;

    @GetMapping
    @Operation(summary = "행정구역 미션 스팟 조회")
    public ApiResponse<MissionSpotMapResponseDTO> getMissionSpots(
            @RequestParam(defaultValue = "11") @Pattern(regexp = "\\d{2}") String regionCode,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(
                MissionSuccessCode.MISSION_SPOTS_FOUND,
                missionSpotQueryService.getMissionSpots(principal.userId(), regionCode)
        );
    }

    @GetMapping("/nearby")
    @Operation(summary = "주변 미션 스팟 조회")
    public ApiResponse<NearbyMissionSpotListResponseDTO> getNearbyMissionSpots(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "5") @Min(1) @Max(100) int limit,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(
                MissionSuccessCode.NEARBY_MISSION_SPOTS_FOUND,
                missionSpotQueryService.getNearbyMissionSpots(
                        principal.userId(), latitude, longitude, limit
                )
        );
    }

    @GetMapping("/{districtCode}/missions")
    @Operation(
            summary = "행정구역 미션 목록 조회",
            description = """
                    미션 목록(GET /api/missions)과 같은 커서 규칙이다: 첫 요청은 cursor 없이, nextCursor를 그대로 넣고,
                    hasNext=false면 nextCursor는 null, 빈 결과는 빈 배열. 커서에 좌표 유무와 정렬 모드가 묶여 있고 랜덤순(좌표 없음)
                    커서는 날짜(KST)가 바뀌면 MISSION_400_002로 거부되니 그때는 cursor 없이 처음부터 다시 요청한다.
                    """
    )
    public ApiResponse<MissionListResponseDTO> getDistrictMissions(
            @PathVariable @Pattern(regexp = "\\d{5}") String districtCode,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) @Size(max = 500) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(
                MissionSuccessCode.DISTRICT_MISSIONS_FOUND,
                missionSpotQueryService.getDistrictMissions(
                        principal.userId(),
                        districtCode,
                        latitude,
                        longitude,
                        cursor,
                        size
                )
        );
    }
}
