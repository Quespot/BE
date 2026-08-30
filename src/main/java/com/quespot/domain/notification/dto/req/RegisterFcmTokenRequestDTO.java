package com.quespot.domain.notification.dto.req;

import com.quespot.domain.notification.enums.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterFcmTokenRequestDTO(
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        String token,

        @NotNull(message = "기기 타입은 필수입니다.")
        DeviceType deviceType
) {
}
