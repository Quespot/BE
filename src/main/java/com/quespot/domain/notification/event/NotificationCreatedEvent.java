package com.quespot.domain.notification.event;

import com.quespot.domain.notification.enums.NotificationReferenceType;
import com.quespot.domain.notification.enums.NotificationType;

import java.util.LinkedHashMap;
import java.util.Map;

// NotificationService.notify()가 발행. NotificationPushListener가 AFTER_COMMIT에 받아 푸시한다.
public record NotificationCreatedEvent(
        Long notificationId,
        Long userId,
        String title,
        String body,
        NotificationType type,
        NotificationReferenceType referenceType,
        Long referenceId
) {

    // FCM data payload. 앱이 딥링크에 쓴다. 값은 전부 문자열이어야 한다(FCM 제약).
    public Map<String, String> toData() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", type.name());
        data.put("notificationId", String.valueOf(notificationId));
        if (referenceType != null && referenceId != null) {
            data.put("referenceType", referenceType.name());
            data.put("referenceId", String.valueOf(referenceId));
        }
        return data;
    }
}
