package com.quespot.domain.user.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import com.quespot.domain.user.enums.LoginProvider;

import java.time.LocalDateTime;

public record LoginMethodResponseDTO(
        LoginProvider provider,
        boolean linked,
        @Schema(description = "linked=false면 null", nullable = true)
        String maskedEmail,
        @Schema(description = "linked=false면 null", nullable = true)
        LocalDateTime linkedAt,
        boolean canUnlink
) {
}
