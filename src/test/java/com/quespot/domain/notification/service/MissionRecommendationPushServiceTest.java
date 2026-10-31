package com.quespot.domain.notification.service;

import com.quespot.domain.notification.config.NotificationRecommendationProperties;
import com.quespot.domain.notification.entity.FcmToken;
import com.quespot.domain.notification.enums.DeviceType;
import com.quespot.domain.notification.enums.RecommendationOutcome;
import com.quespot.domain.notification.repository.FcmTokenRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionRecommendationPushServiceTest {

    private final FcmTokenRepository fcmTokenRepository = mock(FcmTokenRepository.class);
    private final MissionRecommendationNotifier notifier = mock(MissionRecommendationNotifier.class);
    private final NotificationRecommendationProperties properties =
            new NotificationRecommendationProperties(true, "0 0 11 * * *", "Asia/Seoul", 3000, 7);
    private final MissionRecommendationPushService service =
            new MissionRecommendationPushService(fcmTokenRepository, notifier, properties);

    private static FcmToken located(Long userId, String token, String lat, String lng, LocalDateTime at) {
        FcmToken fcmToken = FcmToken.register(userId, token, DeviceType.ANDROID);
        fcmToken.updateLocation(new BigDecimal(lat), new BigDecimal(lng), at);
        return fcmToken;
    }

    @Test
    void usesFreshestTokenPerUserAndCallsNotifierOncePerUser() {
        LocalDateTime older = LocalDateTime.of(2026, 9, 10, 9, 0);
        LocalDateTime newer = LocalDateTime.of(2026, 9, 11, 9, 0);
        when(fcmTokenRepository.findLocatedTokensOfPushEnabledUsers(any(LocalDateTime.class))).thenReturn(List.of(
                located(1L, "a-old", "37.1", "127.1", older),
                located(1L, "a-new", "37.2", "127.2", newer),
                located(2L, "b", "35.1", "129.0", older)
        ));
        when(notifier.recommend(any(), any(), any())).thenReturn(RecommendationOutcome.SENT);

        service.recommendToAll();

        verify(notifier).recommend(eq(1L), eq(new BigDecimal("37.2")), eq(new BigDecimal("127.2")));
        verify(notifier).recommend(eq(2L), eq(new BigDecimal("35.1")), eq(new BigDecimal("129.0")));
        verify(notifier, never()).recommend(eq(1L), eq(new BigDecimal("37.1")), any());
    }

    @Test
    void continuesWithNextUserWhenOneFails() {
        LocalDateTime at = LocalDateTime.of(2026, 9, 11, 9, 0);
        when(fcmTokenRepository.findLocatedTokensOfPushEnabledUsers(any(LocalDateTime.class))).thenReturn(List.of(
                located(1L, "a", "37.1", "127.1", at),
                located(2L, "b", "37.1", "127.1", at)
        ));
        when(notifier.recommend(eq(1L), any(), any())).thenThrow(new IllegalStateException("boom"));
        when(notifier.recommend(eq(2L), any(), any())).thenReturn(RecommendationOutcome.SENT);

        service.recommendToAll();

        verify(notifier).recommend(eq(2L), any(), any());
    }
}
