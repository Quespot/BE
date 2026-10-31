package com.quespot.domain.reward.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_badges",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_badge", columnNames = {"user_id", "badge_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    private UserBadge(Long userId, Badge badge) {
        this.userId = userId;
        this.badge = badge;
        this.acquiredAt = LocalDateTime.now();
    }

    public static UserBadge acquire(Long userId, Badge badge) {
        return new UserBadge(userId, badge);
    }
}
