package com.quespot.domain.mission.entity;

import com.quespot.domain.mission.enums.MissionCourseStatus;
import com.quespot.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 사용자가 미션 목록에서 anchor 미션 하나를 골라 "코스 생성"을 누르면
// MissionCourseGenerationService가 규칙 기반으로 자동 생성한다(#39 재설계).
// 운영자 SQL 등록 경로는 폐기됐다 — 정적 팩토리 generate()로만 만든다.
@Entity
@Table(name = "mission_courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionCourse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anchor_mission_id", nullable = false)
    private Mission anchorMission;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Column(name = "region_code", length = 10)
    private String regionCode;

    @Column(name = "total_reward_point", nullable = false)
    private Integer totalRewardPoint;

    @Column(name = "bonus_point", nullable = false)
    private Integer bonusPoint;

    @Column(name = "mission_count", nullable = false)
    private Integer missionCount;

    @Column(name = "estimated_minutes", nullable = false)
    private Integer estimatedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MissionCourseStatus status;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    private MissionCourse(
            Mission anchorMission, Long createdByUserId, int missionCount,
            String name, String description, String coverImageUrl, String regionCode,
            int totalRewardPoint, int bonusPoint, int estimatedMinutes
    ) {
        this.anchorMission = anchorMission;
        this.createdByUserId = createdByUserId;
        this.missionCount = missionCount;
        this.name = name;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.regionCode = regionCode;
        this.totalRewardPoint = totalRewardPoint;
        this.bonusPoint = bonusPoint;
        this.estimatedMinutes = estimatedMinutes;
        this.status = MissionCourseStatus.ACTIVE;
        this.isPublic = false;
    }

    public static MissionCourse generate(
            Mission anchorMission, Long createdByUserId, int missionCount,
            String name, String description, String coverImageUrl, String regionCode,
            int totalRewardPoint, int bonusPoint, int estimatedMinutes
    ) {
        return new MissionCourse(
                anchorMission, createdByUserId, missionCount, name, description, coverImageUrl, regionCode,
                totalRewardPoint, bonusPoint, estimatedMinutes
        );
    }
}
