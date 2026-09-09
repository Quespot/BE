package com.quespot.domain.mission.entity;

import com.quespot.domain.mission.enums.MissionCourseStatus;
import com.quespot.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 운영자가 SQL로 직접 등록한다. 정적 팩토리가 없다 — 자바에서 만들 경로가 없고
// 조회만 한다. total_reward_point/mission_count/estimated_minutes는 목록
// 화면 표시용 캐시라 코드에서 재계산하지 않는다(운영자가 등록 시점에 채운다).
@Entity
@Table(name = "mission_courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionCourse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

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
}
