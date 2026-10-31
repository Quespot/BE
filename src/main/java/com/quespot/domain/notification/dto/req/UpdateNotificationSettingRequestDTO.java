package com.quespot.domain.notification.dto.req;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationSettingRequestDTO(
        @NotNull(message = "알림 설정값은 필수입니다.")
        Boolean pushEnabled
) {
}
