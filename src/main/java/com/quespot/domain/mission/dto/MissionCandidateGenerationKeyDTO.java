package com.quespot.domain.mission.dto;

import com.quespot.domain.mission.enums.MissionTemplate;

public record MissionCandidateGenerationKeyDTO(
        Long spotId,
        MissionTemplate templateCode
) {
}
