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
        name = "stamps",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stamps_code", columnNames = "code")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "region_code", nullable = false, length = 5)
    private String regionCode;

    @Column(name = "icon_url", columnDefinition = "TEXT")
    private String iconUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    // 수집 가능 여부. 현재는 서울만 true이고, 나머지 시도는 미션이 아직 없어 잠금으로 노출한다.
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    private Stamp(
            String code,
            String name,
            String regionCode,
            int sortOrder,
            boolean isActive
    ) {
        this.code = code;
        this.name = name;
        this.regionCode = regionCode;
        this.sortOrder = sortOrder;
        this.isActive = isActive;
    }

    public static Stamp seed(
            String code,
            String name,
            String regionCode,
            int sortOrder,
            boolean isActive
    ) {
        return new Stamp(code, name, regionCode, sortOrder, isActive);
    }
}
