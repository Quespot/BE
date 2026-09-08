package com.quespot.domain.tour.controller;

import com.quespot.domain.tour.dto.res.TourSyncResponseDTO;
import com.quespot.domain.tour.exception.code.TourSyncSuccessCode;
import com.quespot.domain.tour.service.TourSyncOrchestrator;
import com.quespot.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/tour-sync")
@Tag(name = "Admin Tour Sync", description = "관리자 TourAPI 동기화 API")
public class AdminTourSyncController {

    private final TourSyncOrchestrator tourSyncOrchestrator;

    @PostMapping
    @Operation(
            summary = "TourAPI 동기화 수동 실행",
            description = "원천 데이터 수집, Spot 정제, 미션 후보 생성을 순서대로 실행합니다."
    )
    public ApiResponse<TourSyncResponseDTO> synchronize() {
        return ApiResponse.of(TourSyncSuccessCode.SYNC_COMPLETED, tourSyncOrchestrator.synchronize());
    }
}
