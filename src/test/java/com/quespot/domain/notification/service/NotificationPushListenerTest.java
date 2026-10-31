package com.quespot.domain.notification.service;

import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.enums.DeviceType;
import com.quespot.domain.notification.enums.NotificationReferenceType;
import com.quespot.domain.notification.enums.NotificationType;
import com.quespot.domain.notification.event.NotificationCreatedEvent;
import com.quespot.domain.notification.repository.FcmTokenRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationPushListenerTest {

    private final FcmTokenRepository fcmTokenRepository = mock(FcmTokenRepository.class);
    private final FcmPushSender fcmPushSender = mock(FcmPushSender.class);
    private final FcmTokenCleaner fcmTokenCleaner = mock(FcmTokenCleaner.class);
    private final NotificationPushListener listener =
            new NotificationPushListener(fcmTokenRepository, fcmPushSender, fcmTokenCleaner);

    private final NotificationCreatedEvent event = new NotificationCreatedEvent(
            10L, 7L, "제목", "본문", NotificationType.MISSION_RECOMMENDATION,
            NotificationReferenceType.MISSION, 99L);

    @Test
    void doesNotSendWhenUserHasNoTokens() {
        when(fcmTokenRepository.findAllByUserId(7L)).thenReturn(List.of());

        listener.onNotificationCreated(event);

        verify(fcmPushSender, never()).send(anyList(), anyString(), any(), anyMap());
    }

    @Test
    void sendsToAllTokensAndDeletesInvalidOnes() {
        List<FcmToken> tokens = List.of(
                FcmToken.register(7L, "a", DeviceType.ANDROID),
                FcmToken.register(7L, "b", DeviceType.IOS));
        when(fcmTokenRepository.findAllByUserId(7L)).thenReturn(tokens);
        when(fcmPushSender.send(eq(tokens), eq("제목"), eq("본문"), eq(event.toData())))
                .thenReturn(new PushResult(1, List.of("b")));

        listener.onNotificationCreated(event);

        verify(fcmTokenCleaner).deleteByTokens(List.of("b"));
    }

    @Test
    void skipsCleanerWhenNothingInvalid() {
        when(fcmTokenRepository.findAllByUserId(7L))
                .thenReturn(List.of(FcmToken.register(7L, "a", DeviceType.ANDROID)));
        when(fcmPushSender.send(anyList(), anyString(), any(), anyMap()))
                .thenReturn(new PushResult(1, List.of()));

        listener.onNotificationCreated(event);

        verify(fcmTokenCleaner, never()).deleteByTokens(anyList());
    }

    @Test
    void swallowsExceptionsFromSendPath() {
        when(fcmTokenRepository.findAllByUserId(7L)).thenThrow(new IllegalStateException("db down"));

        listener.onNotificationCreated(event); // must not throw
    }
}
