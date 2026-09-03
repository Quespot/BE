package com.quespot.domain.reward.entity;

import com.quespot.domain.reward.enums.ActivityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reward_activities",
        indexes = {
                // 커서 페이지네이션(ORDER BY id DESC WHERE user_id = ?)이 인덱스만으로 처리되도록.
                // 스키마의 ix_ra_user(user_id, created_at)는 이 쿼리 정렬 기준(id)과 안 맞아 filesort를 유발한다.
                @Index(name = "ix_ra_user_id", columnList = "user_id, id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType;

    @Column(name = "title", nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_transaction_id")
    private PointTransaction pointTransaction;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
