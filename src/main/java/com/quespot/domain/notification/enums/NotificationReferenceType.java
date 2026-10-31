package com.quespot.domain.notification.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "딥링크 대상 유형. MISSION이면 referenceId는 missions.id")
public enum NotificationReferenceType {
    MISSION
}
