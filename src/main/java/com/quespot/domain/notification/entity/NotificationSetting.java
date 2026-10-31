package com.quespot.domain.notification.entity;

import com.quespot.domain.user.entity.User;
import com.quespot.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    private NotificationSetting(User user, boolean pushEnabled) {
        this.user = user;
        this.pushEnabled = pushEnabled;
    }

    public static NotificationSetting create(User user, boolean pushEnabled) {
        return new NotificationSetting(user, pushEnabled);
    }

    public void updatePushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }
}
