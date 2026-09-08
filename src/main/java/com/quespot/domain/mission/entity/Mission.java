package com.quespot.domain.mission.entity;

import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.MissionSource;
import com.quespot.domain.mission.enums.MissionStatus;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.global.entity.BaseEntity;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "missions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_missions_candidate", columnNames = "candidate_id")
        },
        indexes = {
                @Index(name = "ix_missions_status_category", columnList = "status, category"),
                @Index(name = "ix_missions_spot", columnList = "spot_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private MissionCandidate candidate;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private MissionCategory category;

    @Column(name = "reward_point", nullable = false)
    private Integer rewardPoint;

    @Column(name = "estimated_minutes", nullable = false)
    private Integer estimatedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MissionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private MissionSource source;

    @Column(name = "snapshot_name", nullable = false)
    private String snapshotName;

    @Column(name = "snapshot_address")
    private String snapshotAddress;

    @Column(name = "snapshot_latitude", nullable = false, precision = 13, scale = 10)
    private BigDecimal snapshotLatitude;

    @Column(name = "snapshot_longitude", nullable = false, precision = 13, scale = 10)
    private BigDecimal snapshotLongitude;

    @Column(name = "snapshot_image_url", columnDefinition = "TEXT")
    private String snapshotImageUrl;

    private Mission(MissionCandidate candidate) {
        Spot spot = candidate.getSpot();
        this.spot = spot;
        this.candidate = candidate;
        this.title = candidate.getGeneratedTitle();
        this.description = candidate.getGeneratedDescription();
        this.category = candidate.getSuggestedCategory();
        this.rewardPoint = candidate.getSuggestedRewardPoint();
        this.estimatedMinutes = candidate.getSuggestedEstimatedMinutes();
        this.status = MissionStatus.ACTIVE;
        this.source = MissionSource.TOUR_API;
        this.snapshotName = spot.getName();
        this.snapshotAddress = spot.getAddress();
        this.snapshotLatitude = spot.getLatitude();
        this.snapshotLongitude = spot.getLongitude();
        this.snapshotImageUrl = preferredImage(spot);
    }

    public static Mission publish(MissionCandidate candidate) {
        return new Mission(candidate);
    }

    private static String preferredImage(Spot spot) {
        return hasText(spot.getImageUrl()) ? spot.getImageUrl() : spot.getThumbnailUrl();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
