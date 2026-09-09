package com.quespot.domain.user.service;

import java.time.LocalDateTime;

public record OAuth2ProviderToken(
        String accessToken,
        String refreshToken,
        LocalDateTime accessTokenExpiresAt
) {
}
