package com.quespot.domain.like.entity;

import com.quespot.domain.like.enums.LikeTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// target_type + target_id 다형성 참조라 FK가 없다 — 의도된 설계(CLAUDE.md).
// 등록은 LikeRepository.upsert(네이티브 ON DUPLICATE KEY UPDATE)로만 한다.
// 이 엔티티는 조회·삭제 매핑과 DDL 생성용이다.
@Entity
@Table(
        name = "likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_likes", columnNames = {"user_id", "target_type", "target_id"})
        },
        indexes = {
                @Index(name = "ix_likes_target", columnList = "target_type, target_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private LikeTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
