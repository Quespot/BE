package com.quespot.domain.user.dto.res;

public record OAuth2LinkStartResponseDTO(
        String authorizationUrl,
        long expiresInSeconds
) {
}
