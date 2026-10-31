package com.quespot.domain.notification.service;

import java.util.List;

// FcmPushSender 결과. invalidTokens는 UNREGISTERED/INVALID_ARGUMENT로 실패한 토큰 값 —
// 호출부(NotificationPushListener)가 즉시 삭제한다.
public record PushResult(int successCount, List<String> invalidTokens) {

    public static PushResult skipped() {
        return new PushResult(0, List.of());
    }
}
