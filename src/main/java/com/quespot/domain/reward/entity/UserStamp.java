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
        name = "user_stamps",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_stamp", columnNames = {"user_id", "stamp_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stamp_id", nullable = false)
    private Stamp stamp;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    private UserStamp(Long userId, Stamp stamp) {
        this.userId = userId;
        this.stamp = stamp;
        this.acquiredAt = LocalDateTime.now();
    }

    public static UserStamp acquire(Long userId, Stamp stamp) {
        return new UserStamp(userId, stamp);
    }
}
