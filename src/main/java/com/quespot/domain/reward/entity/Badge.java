package com.quespot.domain.reward.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "badges",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_badges_code", columnNames = "code")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "icon_url", columnDefinition = "TEXT")
    private String iconUrl;

    // {metric, scope?: {regionCode}, threshold} — AchievementService가 BadgeCondition으로
    // 파싱해 판정한다(#50). 마스터 정의는 RewardMasterDataSeeder가 코드 기준으로 맞춘다.
    @Column(name = "condition_json", nullable = false, columnDefinition = "json")
    private String conditionJson;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    private Badge(
            String code,
            String name,
            String description,
            String conditionJson,
            int sortOrder
    ) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.conditionJson = conditionJson;
        this.sortOrder = sortOrder;
        this.isActive = true;
    }

    public static Badge seed(
            String code,
            String name,
            String description,
            String conditionJson,
            int sortOrder
    ) {
        return new Badge(code, name, description, conditionJson, sortOrder);
    }

    // 시더가 기동 시 마스터 정의(condition_json 등)를 코드 기준으로 맞출 때 쓴다.
    public void updateMaster(String name, String description, String conditionJson, int sortOrder) {
        this.name = name;
        this.description = description;
        this.conditionJson = conditionJson;
        this.sortOrder = sortOrder;
        this.isActive = true;
    }

    // 마스터 목록에서 빠진 배지(부산 탐험 보류 등)는 user_badges FK 때문에
    // 삭제하지 않고 비활성으로 내린다. 판정·분모는 is_active=true만 본다.
    public void deactivate() {
        this.isActive = false;
    }
}
