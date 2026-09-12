package com.quespot.domain.notification.dto;

import com.quespot.domain.notification.enums.NotificationReferenceType;
import com.quespot.domain.notification.enums.NotificationType;

// 서비스 내부용. 컨트롤러로 나가지 않는다.
public record NotificationCommand(
        Long userId,
        NotificationType type,
        String title,
        String body,
        NotificationReferenceType referenceType,
        Long referenceId
) {
}
