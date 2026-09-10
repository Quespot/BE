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
//
// image_url 컬럼(스키마는 안 바꿈, #45)엔 실제로는 URL이 아니라 S3 objectKey를
// 저장한다 — 버킷이 비공개(Public Access Block 유지, 이슈 #42)라 저장 시점엔
// 아직 렌더링 가능한 URL이 없고, 조회 시점에 FileService.createPresignedDownloadUrl로
// 매번 새로 만든다. 그래서 자바 필드/게터는 imageKey로 정직하게 이름 붙인다.
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
    private String imageKey;

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
            String imageKey,
            String caption,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalDateTime takenAt
    ) {
        this.attempt = attempt;
        this.imageKey = imageKey;
        this.caption = caption;
        this.latitude = latitude;
        this.longitude = longitude;
        this.takenAt = takenAt;
        this.createdAt = LocalDateTime.now();
    }

    public static MissionPhoto record(
            MissionAttempt attempt,
            String imageKey,
            String caption,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalDateTime takenAt
    ) {
        return new MissionPhoto(attempt, imageKey, caption, latitude, longitude, takenAt);
    }
}
