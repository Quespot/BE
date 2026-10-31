package com.quespot.domain.notification.enums;

import io.swagger.v3.oas.annotations.media.Schema;

// docs/quespot_schema.sql의 notifications.type 주석(MISSION_REMINDER/REWARD/BADGE/LIKE/NOTICE)에는
// 없는 값이다 — 이번 이슈의 트리거는 주변 미션 추천 하나뿐이라 그것만 둔다(#57).
@Schema(description = "MISSION_RECOMMENDATION: 주변 미션 추천(스케줄러 발송)")
public enum NotificationType {
    MISSION_RECOMMENDATION
}
