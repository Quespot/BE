package com.quespot.domain.mission.controller;

import com.quespot.domain.mission.dto.res.MissionDetailResponseDTO;
import com.quespot.domain.mission.dto.res.MissionListResponseDTO;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.exception.code.MissionSuccessCode;
import com.quespot.domain.mission.service.MissionQueryService;
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
@Tag(name = "Mission", description = "미션 API")
public class MissionController {

    private final MissionQueryService missionQueryService;

    @GetMapping
    @Operation(summary = "미션 목록 조회")
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
    @Operation(summary = "미션 상세 조회")
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
}
