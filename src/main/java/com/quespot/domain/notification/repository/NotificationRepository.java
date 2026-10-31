package com.quespot.domain.notification.repository;

import com.quespot.domain.notification.entity.Notification;
import com.quespot.domain.notification.enums.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(Long userId, NotificationType type, LocalDateTime since);

    @Query("""
            select n from Notification n
             where n.userId = :userId
               and (:cursor is null or n.id < :cursor)
             order by n.id desc
            """)
    List<Notification> findPage(@Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

    long countByUserIdAndReadAtIsNull(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    // 갱신 0행 = 없거나 남의 것이거나 이미 읽음. 호출부가 existsByIdAndUserId로 구분한다.
    @Modifying
    @Query("update Notification n set n.readAt = :now where n.id = :id and n.userId = :userId and n.readAt is null")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
