package com.quespot.domain.user.dto.res;

import com.quespot.domain.user.enums.LoginProvider;

import java.time.LocalDateTime;

public record LoginMethodResponseDTO(
        LoginProvider provider,
        boolean linked,
        String maskedEmail,
        LocalDateTime linkedAt,
        boolean canUnlink
) {
}
