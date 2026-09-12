package com.quespot.domain.notification.dto.res;

import com.quespot.domain.notification.enums.NotificationReferenceType;
import com.quespot.domain.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Long id,
        NotificationType type,
        String title,
        @Schema(description = "본문. 없으면 null", nullable = true)
        String body,
        @Schema(description = "딥링크 대상 유형. 없으면 null", nullable = true)
        NotificationReferenceType referenceType,
        @Schema(description = "딥링크 대상 id. referenceType이 null이면 null", nullable = true)
        Long referenceId,
        @Schema(description = "읽음 여부")
        boolean read,
        LocalDateTime createdAt
) {
}
