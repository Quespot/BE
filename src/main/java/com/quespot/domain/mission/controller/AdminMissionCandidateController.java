package com.quespot.domain.mission.controller;

import com.quespot.domain.mission.dto.req.RejectMissionCandidateRequestDTO;
import com.quespot.domain.mission.dto.req.UpdateMissionCandidateRequestDTO;
import com.quespot.domain.mission.dto.res.MissionCandidateGenerationResponseDTO;
import com.quespot.domain.mission.dto.res.MissionCandidateBatchResponseDTO;
import com.quespot.domain.mission.dto.res.MissionCandidateListResponseDTO;
import com.quespot.domain.mission.dto.res.MissionCandidateResponseDTO;
import com.quespot.domain.mission.enums.MissionCandidateStatus;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.exception.code.MissionSuccessCode;
import com.quespot.domain.mission.service.MissionCandidateAdminService;
import com.quespot.domain.mission.service.MissionCandidateGenerator;
import com.quespot.global.apiPayload.ApiResponse;
import com.quespot.global.security.principal.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/mission-candidates")
@Tag(name = "Admin Mission Candidate", description = "관리자 미션 후보 생성·검수·발행 API")
public class AdminMissionCandidateController {

    private final MissionCandidateGenerator missionCandidateGenerator;
    private final MissionCandidateAdminService missionCandidateAdminService;

    @PostMapping("/generate")
    @Operation(summary = "미션 후보 일괄 생성")
    public ApiResponse<MissionCandidateGenerationResponseDTO> generateCandidates(
            @RequestParam MissionCategory category
    ) {
        return ApiResponse.of(
                MissionSuccessCode.CANDIDATES_GENERATED,
                missionCandidateGenerator.generate(category)
        );
    }

    @GetMapping
    @Operation(summary = "미션 후보 목록 조회")
    public ApiResponse<MissionCandidateListResponseDTO> getCandidates(
            @RequestParam(required = false) MissionCandidateStatus status,
            @RequestParam(required = false) MissionCategory category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.of(
                MissionSuccessCode.CANDIDATES_FOUND,
                missionCandidateAdminService.getCandidates(status, category, page, size)
        );
    }

    @GetMapping("/{candidateId}")
    @Operation(summary = "미션 후보 상세 조회")
    public ApiResponse<MissionCandidateResponseDTO> getCandidate(
            @PathVariable Long candidateId
    ) {
        return ApiResponse.of(
                MissionSuccessCode.CANDIDATE_FOUND,
                missionCandidateAdminService.getCandidate(candidateId)
        );
    }

    @PatchMapping("/{candidateId}")
    @Operation(summary = "미션 후보 수정")
    public ApiResponse<MissionCandidateResponseDTO> updateCandidate(
            @PathVariable Long candidateId,
            @Valid @RequestBody UpdateMissionCandidateRequestDTO request
    ) {
        return ApiResponse.of(
                MissionSuccessCode.CANDIDATE_UPDATED,
                missionCandidateAdminService.updateCandidate(candidateId, request)
        );
    }

    @PostMapping("/approve")
    @Operation(
            summary = "미션 후보 일괄 승인",
            description = "선택한 카테고리의 모든 DRAFT 후보를 승인합니다."
    )
    public ApiResponse<MissionCandidateBatchResponseDTO> approveCandidates(
            @RequestParam MissionCategory category,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return ApiResponse.of(
                MissionSuccessCode.CANDIDATES_APPROVED,
                missionCandidateAdminService.approveAll(category, principal.userId())
        );
    }

    @PostMapping("/{candidateId}/reject")
    @Operation(summary = "미션 후보 반려")
    public ApiResponse<MissionCandidateResponseDTO> rejectCandidate(
            @PathVariable Long candidateId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody RejectMissionCandidateRequestDTO request
    ) {
        return ApiResponse.of(
                MissionSuccessCode.CANDIDATE_REJECTED,
                missionCandidateAdminService.reject(candidateId, principal.userId(), request)
        );
    }

    @PostMapping("/publish")
    @Operation(
            summary = "미션 일괄 발행",
            description = "선택한 카테고리의 모든 APPROVED 후보를 미션으로 발행합니다."
    )
    public ResponseEntity<ApiResponse<MissionCandidateBatchResponseDTO>> publishMissions(
            @RequestParam MissionCategory category
    ) {
        MissionCandidateBatchResponseDTO result = missionCandidateAdminService.publishAll(category);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(MissionSuccessCode.MISSIONS_PUBLISHED, result));
    }
}
