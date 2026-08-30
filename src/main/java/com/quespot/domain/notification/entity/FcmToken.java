package com.quespot.domain.notification.entity;

import com.quespot.domain.notification.enums.DeviceType;
import com.quespot.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "fcm_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fcm_tokens_token", columnNames = "token")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, length = 255)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    private FcmToken(Long userId, String token, DeviceType deviceType) {
        this.userId = userId;
        this.token = token;
        this.deviceType = deviceType;
    }

    public static FcmToken register(Long userId, String token, DeviceType deviceType) {
        return new FcmToken(userId, token, deviceType);
    }

    public void reassignTo(Long userId, DeviceType deviceType) {
        this.userId = userId;
        this.deviceType = deviceType;
    }
}
