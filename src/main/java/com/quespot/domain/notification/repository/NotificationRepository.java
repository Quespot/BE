package com.quespot.domain.notification.repository;

import com.quespot.domain.notification.entity.Notification;
import com.quespot.domain.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(Long userId, NotificationType type, LocalDateTime since);
}
