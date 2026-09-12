package com.quespot.domain.notification.service;

import com.quespot.domain.notification.dto.res.NotificationListResponseDTO;
import com.quespot.domain.notification.entity.Notification;
import com.quespot.domain.notification.enums.NotificationReferenceType;
import com.quespot.domain.notification.enums.NotificationType;
import com.quespot.domain.notification.exception.NotificationException;
import com.quespot.domain.notification.exception.code.NotificationErrorCode;
import com.quespot.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationQueryServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final NotificationQueryService service = new NotificationQueryService(notificationRepository);

    private static Notification notification(long id) {
        Notification n = Notification.create(1L, NotificationType.MISSION_RECOMMENDATION, "t" + id, null,
                NotificationReferenceType.MISSION, 5L);
        ReflectionTestUtils.setField(n, "id", id);
        ReflectionTestUtils.setField(n, "createdAt", LocalDateTime.of(2026, 9, 12, 11, 0));
        return n;
    }

    @Test
    void pagesWithSizePlusOneAndExposesNextCursor() {
        when(notificationRepository.findPage(1L, null, PageRequest.of(0, 3)))
                .thenReturn(List.of(notification(30), notification(29), notification(28)));

        NotificationListResponseDTO page = service.getNotifications(1L, null, 2);

        assertThat(page.notifications()).extracting("id").containsExactly(30L, 29L);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isEqualTo(29L);
    }

    @Test
    void lastPageHasNullCursor() {
        when(notificationRepository.findPage(1L, 29L, PageRequest.of(0, 3)))
                .thenReturn(List.of(notification(28)));

        NotificationListResponseDTO page = service.getNotifications(1L, 29L, 2);

        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.notifications().get(0).read()).isFalse();
    }

    @Test
    void markAsReadIsIdempotentWhenAlreadyRead() {
        when(notificationRepository.markAsRead(eq(10L), eq(1L), any(LocalDateTime.class))).thenReturn(0);
        when(notificationRepository.existsByIdAndUserId(10L, 1L)).thenReturn(true);

        assertThatCode(() -> service.markAsRead(1L, 10L)).doesNotThrowAnyException();
    }

    @Test
    void markAsReadThrowsNotFoundForOtherUsersNotification() {
        when(notificationRepository.markAsRead(eq(10L), eq(1L), any(LocalDateTime.class))).thenReturn(0);
        when(notificationRepository.existsByIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.markAsRead(1L, 10L))
                .isInstanceOf(NotificationException.class)
                .satisfies(e -> assertThat(((NotificationException) e).getErrorCode())
                        .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }
}
