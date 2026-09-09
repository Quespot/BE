package com.quespot.domain.mission.controller;

import com.quespot.domain.mission.dto.res.MissionArchiveListResponseDTO;
import com.quespot.domain.mission.exception.code.MissionSuccessCode;
import com.quespot.domain.mission.service.MissionArchiveQueryService;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "MissionArchive", description = "미션 사진 아카이브 API")
public class MissionArchiveController {

    private final MissionArchiveQueryService missionArchiveQueryService;

    @GetMapping("/api/users/me/archives")
    @Operation(
            summary = "내 미션 사진 아카이브 목록 조회",
            description = "GPS 인증 후 등록한 미션 사진을 최신순으로 조회한다. 지도가 아니라 목록이라 좌표는 응답에 포함하지 않는다."
    )
    public ApiResponse<MissionArchiveListResponseDTO> getArchives(
            @RequestParam(required = false) @Size(max = 500) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(
                MissionSuccessCode.ARCHIVES_FOUND,
                missionArchiveQueryService.getArchives(principal.userId(), cursor, size)
        );
    }
}
