package com.quespot.domain.mission.entity;

import com.quespot.domain.mission.enums.MissionCandidateStatus;
import com.quespot.domain.mission.enums.MissionCategory;
import com.quespot.domain.mission.enums.MissionTemplate;
import com.quespot.domain.spot.entity.Spot;
import com.quespot.domain.user.entity.User;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "mission_candidates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_mission_candidates_generation",
                        columnNames = {"spot_id", "template_code", "generator_version"}
                )
        },
        indexes = {
                @Index(name = "ix_mission_candidates_status", columnList = "status, created_at"),
                @Index(name = "ix_mission_candidates_spot", columnList = "spot_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionCandidate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_code", nullable = false, length = 40)
    private MissionTemplate templateCode;

    @Column(name = "generator_version", nullable = false)
    private Integer generatorVersion;

    @Column(name = "generated_title", nullable = false, length = 200)
    private String generatedTitle;

    @Column(name = "generated_description", nullable = false, columnDefinition = "TEXT")
    private String generatedDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_category", nullable = false, length = 20)
    private MissionCategory suggestedCategory;

    @Column(name = "suggested_reward_point", nullable = false)
    private Integer suggestedRewardPoint;

    @Column(name = "suggested_estimated_minutes", nullable = false)
    private Integer suggestedEstimatedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MissionCandidateStatus status;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    private MissionCandidate(Spot spot, MissionTemplate template, int generatorVersion) {
        this.spot = spot;
        this.templateCode = template;
        this.generatorVersion = generatorVersion;
        this.generatedTitle = template.title(spot.getName());
        this.generatedDescription = template.description(spot.getName());
        this.suggestedCategory = template.getCategory();
        this.suggestedRewardPoint = template.getRewardPoint();
        this.suggestedEstimatedMinutes = template.getEstimatedMinutes();
        this.status = MissionCandidateStatus.DRAFT;
    }

    public static MissionCandidate generate(Spot spot, MissionTemplate template, int generatorVersion) {
        return new MissionCandidate(spot, template, generatorVersion);
    }

    public void update(
            String title,
            String description,
            MissionCategory category,
            Integer rewardPoint,
            Integer estimatedMinutes
    ) {
        if (title != null) {
            this.generatedTitle = title;
        }
        if (description != null) {
            this.generatedDescription = description;
        }
        if (category != null) {
            this.suggestedCategory = category;
        }
        if (rewardPoint != null) {
            this.suggestedRewardPoint = rewardPoint;
        }
        if (estimatedMinutes != null) {
            this.suggestedEstimatedMinutes = estimatedMinutes;
        }
    }

    public void approve(User reviewer) {
        this.status = MissionCandidateStatus.APPROVED;
        this.reason = null;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(User reviewer, String reason) {
        this.status = MissionCandidateStatus.REJECTED;
        this.reason = reason;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }

    public void markPublished() {
        this.status = MissionCandidateStatus.PUBLISHED;
    }
}
