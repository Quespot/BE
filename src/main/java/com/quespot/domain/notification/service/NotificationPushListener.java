package com.quespot.domain.notification.service;

import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.event.NotificationCreatedEvent;
import com.quespot.domain.notification.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

// notifications 행이 커밋된 뒤에만 푸시를 보낸다(외부 HTTP를 트랜잭션 안에서 하지 않는다).
// 호출부에 트랜잭션이 없으면 이 리스너는 돌지 않는다(fallbackExecution 기본 false).
// 어떤 예외도 밖으로 내지 않는다 — 알림 행은 이미 커밋됐고 푸시 실패는 인앱만 남는 상태로 둔다.
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPushListener {

    private final FcmTokenRepository fcmTokenRepository;
    private final FcmPushSender fcmPushSender;
    private final FcmTokenCleaner fcmTokenCleaner;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        try {
            List<FcmToken> tokens = fcmTokenRepository.findAllByUserId(event.userId());
            if (tokens.isEmpty()) {
                log.debug("FCM 토큰이 없어 푸시를 건너뜁니다. userId={}, notificationId={}",
                        event.userId(), event.notificationId());
                return;
            }
            PushResult result = fcmPushSender.send(tokens, event.title(), event.body(), event.toData());
            if (!result.invalidTokens().isEmpty()) {
                fcmTokenCleaner.deleteByTokens(result.invalidTokens());
            }
            log.info("푸시 발송 완료. userId={}, notificationId={}, success={}, invalid={}",
                    event.userId(), event.notificationId(), result.successCount(), result.invalidTokens().size());
        } catch (RuntimeException exception) {
            log.warn("푸시 발송 중 예외. userId={}, notificationId={}",
                    event.userId(), event.notificationId(), exception);
        }
    }
}
