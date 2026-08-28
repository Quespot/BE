package com.quespot.domain.user.dto.res;

public record SignUpResponseDTO(
        Long userId,
        String email,
        String nickname
) {
}
