package com.quespot.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 미션과 무관하게 사용자가 자유롭게 올리는 아카이브 사진(#45 확장). mission_photos는
// attempt_id가 필수라 미션에 안 묶인 사진을 담을 수 없어서 별도 테이블로 둔다.
// GPS 인증이 없는 사진이라 좌표는 안 받는다 — 나중에 필요해지면 컬럼만 추가하면 된다.
@Entity
@Table(name = "archive_photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArchivePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "image_key", nullable = false, columnDefinition = "TEXT")
    private String imageKey;

    @Column(name = "caption")
    private String caption;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private ArchivePhoto(Long userId, String imageKey, String caption) {
        this.userId = userId;
        this.imageKey = imageKey;
        this.caption = caption;
        this.createdAt = LocalDateTime.now();
    }

    public static ArchivePhoto upload(Long userId, String imageKey, String caption) {
        return new ArchivePhoto(userId, imageKey, caption);
    }
}
