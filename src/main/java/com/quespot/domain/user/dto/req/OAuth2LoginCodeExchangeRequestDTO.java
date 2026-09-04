package com.quespot.domain.user.dto.req;

import jakarta.validation.constraints.NotBlank;

public record OAuth2LoginCodeExchangeRequestDTO(
        @NotBlank(message = "소셜 로그인 코드는 필수입니다.")
        String code
) {
}
