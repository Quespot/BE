package com.quespot.domain.mission.dto.res;

public record UnlockConditionResponseDTO(
        boolean locked,
        String message
) {
}
