package com.quespot.global.security.provider;

public record JwtTokenPair(
        String accessToken,
        String refreshToken,
        String sessionId,
        Long refreshTokenExpiresInSeconds
) {
}
