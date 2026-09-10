package com.quespot.domain.user.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
public record TokenReissueResponseDTO(
        @Schema(description = "새 Access Token. Refresh Token 쿠키도 응답에서 함께 갱신된다")
        String accessToken,
        boolean profileCompleted
) {
}
