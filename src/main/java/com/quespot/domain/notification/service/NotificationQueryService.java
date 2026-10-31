package com.quespot.domain.notification.service;

import com.quespot.domain.notification.converter.NotificationConverter;
import com.quespot.domain.notification.dto.res.NotificationListResponseDTO;
import com.quespot.domain.notification.dto.res.NotificationResponseDTO;
import com.quespot.domain.notification.dto.res.UnreadCountResponseDTO;
import com.quespot.domain.notification.entity.Notification;
import com.quespot.domain.notification.exception.NotificationException;
import com.quespot.domain.notification.exception.code.NotificationErrorCode;
import com.quespot.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public NotificationListResponseDTO getNotifications(Long userId, Long cursor, int size) {
        List<Notification> fetched = notificationRepository.findPage(userId, cursor, PageRequest.of(0, size + 1));
        boolean hasNext = fetched.size() > size;
        List<Notification> page = hasNext ? fetched.subList(0, size) : fetched;
        List<NotificationResponseDTO> items = page.stream()
                .map(NotificationConverter::toNotificationResponseDTO)
                .toList();
        Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;
        return new NotificationListResponseDTO(items, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponseDTO countUnread(Long userId) {
        return new UnreadCountResponseDTO(notificationRepository.countByUserIdAndReadAtIsNull(userId));
    }

    // 멱등: 이미 읽은 알림이면 200. 없거나 남의 것이면 404.
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        int updated = notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());
        if (updated == 0 && !notificationRepository.existsByIdAndUserId(notificationId, userId)) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }
}
