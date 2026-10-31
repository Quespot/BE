package com.quespot.domain.notification.entity;

import com.quespot.domain.notification.enums.NotificationReferenceType;
import com.quespot.domain.notification.enums.NotificationType;
import com.quespot.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 인앱 알림. user_id는 FcmToken처럼 Long 값(User 연관 없음). reference_type/id는
// 다형성 딥링크라 FK가 없다(likes와 같은 이유).
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "ix_noti_user", columnList = "user_id, created_at"),
                @Index(name = "ix_noti_unread", columnList = "user_id, read_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    public static final int BODY_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", length = BODY_MAX_LENGTH)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 30)
    private NotificationReferenceType referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    private Notification(Long userId, NotificationType type, String title, String body,
                         NotificationReferenceType referenceType, Long referenceId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    public static Notification create(Long userId, NotificationType type, String title, String body,
                                      NotificationReferenceType referenceType, Long referenceId) {
        return new Notification(userId, type, title, truncate(body), referenceType, referenceId);
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markAsRead(LocalDateTime now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    // 코드 포인트 단위로 자른다 — substring은 이모지 같은 서로게이트 쌍을 반으로 갈라 utf8mb4 저장이 실패한다.
    private static String truncate(String body) {
        if (body == null || body.codePointCount(0, body.length()) <= BODY_MAX_LENGTH) {
            return body;
        }
        return body.substring(0, body.offsetByCodePoints(0, BODY_MAX_LENGTH));
    }
}
