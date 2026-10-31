package com.quespot.domain.mission.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MissionArchiveMapResponseDTO(
        MissionArchiveFootprintResponseDTO footprint,
        @Schema(description = "조회 조건에 해당하는 완료 미션. 비면 빈 배열이다(null 아님)")
        List<CompletedMissionArchiveItemResponseDTO> completedMissions
) {
}
