package com.quespot.domain.notification.service;

import com.quespot.domain.notification.dto.NotificationCommand;
import com.quespot.domain.notification.entity.Notification;
import com.quespot.domain.notification.enums.NotificationReferenceType;
import com.quespot.domain.notification.enums.NotificationType;
import com.quespot.domain.notification.event.NotificationCreatedEvent;
import com.quespot.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final NotificationService notificationService = new NotificationService(notificationRepository, eventPublisher);

    @Test
    void savesRowAndPublishesEventWithSameContent() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 42L);
            return saved;
        });
        NotificationCommand command = new NotificationCommand(
                7L, NotificationType.MISSION_RECOMMENDATION, "제목", "본문",
                NotificationReferenceType.MISSION, 99L);

        Notification result = notificationService.notify(command);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.isRead()).isFalse();
        ArgumentCaptor<NotificationCreatedEvent> captor = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        NotificationCreatedEvent event = captor.getValue();
        assertThat(event.notificationId()).isEqualTo(42L);
        assertThat(event.userId()).isEqualTo(7L);
        assertThat(event.title()).isEqualTo("제목");
        assertThat(event.toData()).containsEntry("type", "MISSION_RECOMMENDATION")
                .containsEntry("referenceType", "MISSION")
                .containsEntry("referenceId", "99")
                .containsEntry("notificationId", "42");
    }

    @Test
    void omitsReferenceKeysFromDataWhenReferenceIsNull() {
        NotificationCreatedEvent event = new NotificationCreatedEvent(
                1L, 2L, "t", null, NotificationType.MISSION_RECOMMENDATION, null, null);

        assertThat(event.toData()).containsOnlyKeys("type", "notificationId");
    }

    @Test
    void truncatesBodyToColumnLength() {
        Notification n = Notification.create(
                1L, NotificationType.MISSION_RECOMMENDATION, "t", "x".repeat(600), null, null);

        assertThat(n.getBody()).hasSize(Notification.BODY_MAX_LENGTH);
    }
}
