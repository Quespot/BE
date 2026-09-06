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

    // 획득 조건 판정은 미션 완료 로직이 선행되어야 하므로 이번 범위 밖.
    // NOT NULL 제약을 맞추기 위한 {metric, scope, threshold} 형태의 placeholder이며,
    // 실제 값은 미션 도메인에서 판정 로직을 만들 때 재정의해야 한다.
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
}
