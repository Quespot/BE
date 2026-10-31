package com.quespot.domain.user.dto.res;

public record SendEmailVerificationCodeResponseDTO(
        String email,
        Integer expiresInMinutes
) {
}
