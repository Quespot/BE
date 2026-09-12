package com.quespot.domain.notification.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.quespot.domain.notification.entity.FcmToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// FirebaseMessaging 래퍼. 빈이 없으면(app.firebase.credentials-path 미설정 — 로컬/CI) 건너뛴다.
// 예외를 밖으로 내지 않는다 — 푸시 실패가 호출부를 실패시키면 안 된다. 트랜잭션 밖에서만 호출할 것.
@Slf4j
@Component
public class FcmPushSender {

    private static final Set<MessagingErrorCode> INVALID_TOKEN_CODES = Set.of(
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT
    );

    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    public FcmPushSender(ObjectProvider<FirebaseMessaging> firebaseMessagingProvider) {
        this.firebaseMessagingProvider = firebaseMessagingProvider;
    }

    public PushResult send(List<FcmToken> tokens, String title, String body, Map<String, String> data) {
        if (tokens.isEmpty()) {
            return PushResult.skipped();
        }
        FirebaseMessaging messaging = firebaseMessagingProvider.getIfAvailable();
        if (messaging == null) {
            log.warn("FirebaseMessaging 빈이 없어 푸시를 건너뜁니다. tokenCount={}", tokens.size());
            return PushResult.skipped();
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens.stream().map(FcmToken::getToken).toList())
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data)
                .build();

        BatchResponse batch;
        try {
            batch = messaging.sendEachForMulticast(message);
        } catch (FirebaseMessagingException exception) {
            log.warn("FCM 일괄 발송 실패. tokenCount={}, code={}",
                    tokens.size(), exception.getMessagingErrorCode(), exception);
            return PushResult.skipped();
        }

        int successCount = 0;
        List<String> invalidTokens = new ArrayList<>();
        List<SendResponse> responses = batch.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);
            FcmToken token = tokens.get(i);
            if (response.isSuccessful()) {
                successCount++;
                continue;
            }
            MessagingErrorCode code = response.getException() == null
                    ? null
                    : response.getException().getMessagingErrorCode();
            if (code != null && INVALID_TOKEN_CODES.contains(code)) {
                invalidTokens.add(token.getToken());
                log.info("무효 FCM 토큰 삭제 예정. fcmTokenId={}, code={}", token.getId(), code);
            } else {
                log.warn("FCM 발송 실패. fcmTokenId={}, code={}", token.getId(), code);
            }
        }
        return new PushResult(successCount, invalidTokens);
    }
}
