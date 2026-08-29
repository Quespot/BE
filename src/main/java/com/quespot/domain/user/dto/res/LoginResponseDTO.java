package com.quespot.domain.user.dto.res;

public record LoginResponseDTO(
        Long userId,
        String accessToken
) {
}
