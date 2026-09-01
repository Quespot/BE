package com.quespot.domain.notification.dto.res;

import com.quespot.domain.notification.enums.DeviceType;

public record RegisterFcmTokenResponseDTO(
        Long id,
        DeviceType deviceType
) {
}
