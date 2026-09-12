package com.quespot.domain.notification.service;

import com.quespot.domain.notification.config.NotificationRecommendationProperties;
import com.quespot.domain.notification.dto.NotificationCommand;
import com.quespot.domain.notification.dto.RecommendableMissionDTO;
import com.quespot.domain.notification.enums.NotificationReferenceType;
import com.quespot.domain.notification.enums.NotificationType;
import com.quespot.domain.notification.enums.RecommendationOutcome;
import com.quespot.domain.notification.repository.NotificationRepository;
import com.quespot.domain.notification.repository.RecommendableMissionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionRecommendationNotifierTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final RecommendableMissionRepository missionRepository = mock(RecommendableMissionRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final NotificationRecommendationProperties properties =
            new NotificationRecommendationProperties(true, "0 0 11 * * *", "Asia/Seoul", 3000, 7);
    private final MissionRecommendationNotifier notifier =
            new MissionRecommendationNotifier(notificationRepository, missionRepository, notificationService, properties);

    private final BigDecimal lat = new BigDecimal("37.5665");
    private final BigDecimal lng = new BigDecimal("126.9780");

    @Test
    void skipsWhenAlreadySentToday() {
        when(notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                eq(1L), eq(NotificationType.MISSION_RECOMMENDATION), any(LocalDateTime.class))).thenReturn(true);

        RecommendationOutcome outcome = notifier.recommend(1L, lat, lng);

        assertThat(outcome).isEqualTo(RecommendationOutcome.ALREADY_SENT_TODAY);
        verify(missionRepository, never()).pickRandomNearby(any(), any(), any(), anyInt());
        verify(notificationService, never()).notify(any());
    }

    @Test
    void skipsWhenNoMissionNearby() {
        when(notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(any(), any(), any()))
                .thenReturn(false);
        when(missionRepository.pickRandomNearby(1L, lat, lng, 3000)).thenReturn(Optional.empty());

        RecommendationOutcome outcome = notifier.recommend(1L, lat, lng);

        assertThat(outcome).isEqualTo(RecommendationOutcome.NO_MISSION_NEARBY);
        verify(notificationService, never()).notify(any());
    }

    @Test
    void notifiesWithMissionContent() {
        when(notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(any(), any(), any()))
                .thenReturn(false);
        when(missionRepository.pickRandomNearby(1L, lat, lng, 3000))
                .thenReturn(Optional.of(new RecommendableMissionDTO(55L, "덕수궁 돌담길 걷기", "덕수궁", 100)));

        RecommendationOutcome outcome = notifier.recommend(1L, lat, lng);

        assertThat(outcome).isEqualTo(RecommendationOutcome.SENT);
        ArgumentCaptor<NotificationCommand> captor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationService).notify(captor.capture());
        NotificationCommand command = captor.getValue();
        assertThat(command.userId()).isEqualTo(1L);
        assertThat(command.type()).isEqualTo(NotificationType.MISSION_RECOMMENDATION);
        assertThat(command.title()).isEqualTo("근처에 미션이 있어요!");
        assertThat(command.body()).isEqualTo("덕수궁에서 '덕수궁 돌담길 걷기' 미션에 도전해 보세요 (+100포인트)");
        assertThat(command.referenceType()).isEqualTo(NotificationReferenceType.MISSION);
        assertThat(command.referenceId()).isEqualTo(55L);
    }
}
