package com.quespot.domain.user.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
public record LoginResponseDTO(
        Long userId,
        @Schema(description = "Authorization: Bearer 헤더에 넣는다. Refresh Token은 body가 아니라 HttpOnly 쿠키(refreshToken)로 내려간다")
        String accessToken,
        @Schema(description = "false면 프로필 생성 화면으로 보낸다")
        boolean profileCompleted
) {
}
