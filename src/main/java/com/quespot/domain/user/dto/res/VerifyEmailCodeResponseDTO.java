package com.quespot.domain.user.dto.res;

public record VerifyEmailCodeResponseDTO(
        String email,
        Boolean verified
) {
}
