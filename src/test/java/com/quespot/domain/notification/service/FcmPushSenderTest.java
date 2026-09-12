package com.quespot.domain.notification.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.enums.DeviceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FcmPushSenderTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<FirebaseMessaging> provider = mock(ObjectProvider.class);
    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final FcmPushSender sender = new FcmPushSender(provider);

    private static FcmToken token(String value) {
        return FcmToken.register(1L, value, DeviceType.ANDROID);
    }

    // SendResponse의 정적 팩토리(fromMessageId/fromException)는 package-private라 밖에서 못 쓴다.
    // Mockito 5 기본 inline mock maker가 final 클래스도 mock하므로 응답 객체를 직접 mock한다.
    private static SendResponse success() {
        SendResponse response = mock(SendResponse.class);
        when(response.isSuccessful()).thenReturn(true);
        return response;
    }

    private static SendResponse failure(MessagingErrorCode code) {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(code);
        SendResponse response = mock(SendResponse.class);
        when(response.isSuccessful()).thenReturn(false);
        when(response.getException()).thenReturn(exception);
        return response;
    }

    @Test
    void skipsWhenFirebaseMessagingBeanIsAbsent() throws Exception {
        when(provider.getIfAvailable()).thenReturn(null);

        PushResult result = sender.send(List.of(token("a")), "t", "b", Map.of());

        assertThat(result.successCount()).isZero();
        assertThat(result.invalidTokens()).isEmpty();
        verify(messaging, never()).sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    void returnsEmptyResultForEmptyTokenList() throws Exception {
        when(provider.getIfAvailable()).thenReturn(messaging);

        PushResult result = sender.send(List.of(), "t", "b", Map.of());

        assertThat(result.successCount()).isZero();
        verify(messaging, never()).sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    void classifiesOnlyUnregisteredAndInvalidArgumentAsInvalid() throws Exception {
        when(provider.getIfAvailable()).thenReturn(messaging);
        // 헬퍼가 내부에서 stubbing하므로 when(...) 인자 안에서 호출하면 중첩 stubbing 오류가 난다. 먼저 만든다.
        List<SendResponse> responses = List.of(
                success(),
                failure(MessagingErrorCode.UNREGISTERED),
                failure(MessagingErrorCode.INVALID_ARGUMENT),
                failure(MessagingErrorCode.UNAVAILABLE)
        );
        BatchResponse batch = mock(BatchResponse.class);
        when(batch.getResponses()).thenReturn(responses);
        when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batch);

        PushResult result = sender.send(
                List.of(token("ok"), token("gone"), token("bad"), token("later")),
                "t", "b", Map.of("type", "X"));

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.invalidTokens()).containsExactly("gone", "bad");
    }

    @Test
    void swallowsWholeBatchFailure() throws Exception {
        when(provider.getIfAvailable()).thenReturn(messaging);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenThrow(exception);

        PushResult result = sender.send(List.of(token("a")), "t", "b", Map.of());

        assertThat(result.successCount()).isZero();
        assertThat(result.invalidTokens()).isEmpty();
    }
}
