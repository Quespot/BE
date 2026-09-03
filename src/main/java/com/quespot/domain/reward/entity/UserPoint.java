package com.quespot.domain.reward.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_points")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPoint {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "balance", nullable = false)
    private Integer balance;

    @Column(name = "total_earned", nullable = false)
    private Integer totalEarned;

    @Column(name = "total_spent", nullable = false)
    private Integer totalSpent;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
