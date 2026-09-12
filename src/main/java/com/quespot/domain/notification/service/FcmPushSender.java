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

// FirebaseMessaging 래퍼. 빈이 없으면(app.firebase.credentials-path 미설정 — 로컬/CI) 건너뛴다.
// 예외를 밖으로 내지 않는다 — 푸시 실패가 호출부를 실패시키면 안 된다. 트랜잭션 밖에서만 호출할 것.
@Slf4j
@Component
public class FcmPushSender {

    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    public FcmPushSender(ObjectProvider<FirebaseMessaging> firebaseMessagingProvider) {
        this.firebaseMessagingProvider = firebaseMessagingProvider;
    }

    // MulticastMessage.build()는 토큰이 500개를 넘으면 IllegalArgumentException을 던진다(FCM 제한).
    // 사용자당 토큰 수에 상한이 없으므로 500개씩 나눠 보내고 결과를 합산한다(PR #58 CodeRabbit 지적).
    static final int MULTICAST_LIMIT = 500;

    public PushResult send(List<FcmToken> tokens, String title, String body, Map<String, String> data) {
        if (tokens.isEmpty()) {
            return PushResult.skipped();
        }
        FirebaseMessaging messaging = firebaseMessagingProvider.getIfAvailable();
        if (messaging == null) {
            log.warn("FirebaseMessaging 빈이 없어 푸시를 건너뜁니다. tokenCount={}", tokens.size());
            return PushResult.skipped();
        }

        Notification notification = Notification.builder().setTitle(title).setBody(body).build();
        int successCount = 0;
        List<String> invalidTokens = new ArrayList<>();
        for (int from = 0; from < tokens.size(); from += MULTICAST_LIMIT) {
            List<FcmToken> chunk = tokens.subList(from, Math.min(from + MULTICAST_LIMIT, tokens.size()));
            PushResult partial = sendChunk(messaging, chunk, notification, data);
            successCount += partial.successCount();
            invalidTokens.addAll(partial.invalidTokens());
        }
        return new PushResult(successCount, invalidTokens);
    }

    private PushResult sendChunk(FirebaseMessaging messaging, List<FcmToken> chunk,
                                 Notification notification, Map<String, String> data) {
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(chunk.stream().map(FcmToken::getToken).toList())
                .setNotification(notification)
                .putAllData(data)
                .build();

        BatchResponse batch;
        try {
            batch = messaging.sendEachForMulticast(message);
        } catch (FirebaseMessagingException exception) {
            log.warn("FCM 일괄 발송 실패. tokenCount={}, code={}",
                    chunk.size(), exception.getMessagingErrorCode(), exception);
            return PushResult.skipped();
        }

        int successCount = 0;
        List<String> invalidTokens = new ArrayList<>();
        List<SendResponse> responses = batch.getResponses();
        int count = Math.min(responses.size(), chunk.size());
        for (int i = 0; i < count; i++) {
            SendResponse response = responses.get(i);
            FcmToken token = chunk.get(i);
            if (response.isSuccessful()) {
                successCount++;
                continue;
            }
            FirebaseMessagingException exception = response.getException();
            MessagingErrorCode code = exception == null ? null : exception.getMessagingErrorCode();
            if (isInvalidToken(code, exception)) {
                invalidTokens.add(token.getToken());
                log.info("무효 FCM 토큰 삭제 예정. fcmTokenId={}, code={}", token.getId(), code);
            } else {
                log.warn("FCM 발송 실패. fcmTokenId={}, code={}", token.getId(), code);
            }
        }
        return new PushResult(successCount, invalidTokens);
    }

    // UNREGISTERED는 항상 토큰 문제. INVALID_ARGUMENT는 토큰이 잘못됐을 때도, 메시지(data 키·크기 등)가
    // 잘못됐을 때도 나온다 — 후자면 사용자의 토큰 전부가 같은 코드로 실패하므로, 메시지에 "token"이
    // 언급될 때만 토큰 삭제 대상으로 본다. 메시지 문제를 토큰 삭제로 덮어버리지 않기 위한 안전장치(#57 리뷰).
    private static boolean isInvalidToken(MessagingErrorCode code, FirebaseMessagingException exception) {
        if (code == MessagingErrorCode.UNREGISTERED) {
            return true;
        }
        if (code != MessagingErrorCode.INVALID_ARGUMENT) {
            return false;
        }
        String message = exception.getMessage();
        return message != null && message.toLowerCase().contains("token");
    }
}
