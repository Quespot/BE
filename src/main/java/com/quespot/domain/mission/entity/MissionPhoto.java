package com.quespot.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 미션당 1장 제약은 unique=true로 DB UNIQUE(attempt_id)를 건다.
// score/phash/ai_provider 같은 AI 판정용 컬럼은 넣지 않는다 — 순수 기록용.
@Entity
@Table(name = "mission_photos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false, unique = true)
    private MissionAttempt attempt;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "caption")
    private String caption;

    // 촬영 위치. 없으면 서비스 레이어에서 mission.snapshotLatitude/Longitude로
    // 폴백해서 넣는다(엔티티는 받은 값을 그대로 저장할 뿐 폴백 로직을 모른다).
    @Column(name = "latitude", precision = 13, scale = 10)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 13, scale = 10)
    private BigDecimal longitude;

    @Column(name = "taken_at")
    private LocalDateTime takenAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private MissionPhoto(
            MissionAttempt attempt,
            String imageUrl,
            String caption,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalDateTime takenAt
    ) {
        this.attempt = attempt;
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.latitude = latitude;
        this.longitude = longitude;
        this.takenAt = takenAt;
        this.createdAt = LocalDateTime.now();
    }

    public static MissionPhoto record(
            MissionAttempt attempt,
            String imageUrl,
            String caption,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalDateTime takenAt
    ) {
        return new MissionPhoto(attempt, imageUrl, caption, latitude, longitude, takenAt);
    }
}
