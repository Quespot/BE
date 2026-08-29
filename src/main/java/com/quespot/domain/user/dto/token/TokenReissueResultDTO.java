package com.quespot.domain.user.dto.token;

import com.quespot.domain.user.dto.res.TokenReissueResponseDTO;

public record TokenReissueResultDTO(
        TokenReissueResponseDTO response,
        String refreshToken,
        Long refreshTokenExpiresInSeconds
) {
}
