package com.quespot.domain.user.dto.token;

import com.quespot.domain.user.dto.res.LoginResponseDTO;

public record LoginResultDTO(
        LoginResponseDTO response,
        String refreshToken,
        Long refreshTokenExpiresInSeconds
) {
}
