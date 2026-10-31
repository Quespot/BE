package com.quespot.domain.spot.entity;

import com.quespot.domain.spot.enums.AppCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// lcls_code는 kto_lcls_systms에 대한 FK가 없다. prefix 문자열 매칭이라 참조
// 무결성이 필요 없고, 마스터에 아직 없는 코드도 매핑을 미리 등록할 수 있어야 한다.
@Entity
@Table(
        name = "category_mappings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_catmap", columnNames = {"lcls_code", "content_type_id", "version"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "lcls_code", nullable = false, length = 9)
    private String lclsCode;

    @Column(name = "content_type_id")
    private Integer contentTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_category", nullable = false, length = 20)
    private AppCategory appCategory;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private CategoryMapping(
            String lclsCode,
            Integer contentTypeId,
            AppCategory appCategory,
            Integer priority,
            Integer version
    ) {
        this.lclsCode = lclsCode;
        this.contentTypeId = contentTypeId;
        this.appCategory = appCategory;
        this.priority = priority;
        this.version = version;
        this.createdAt = LocalDateTime.now();
    }

    public static CategoryMapping seed(
            String lclsCode,
            Integer contentTypeId,
            AppCategory appCategory,
            Integer priority,
            Integer version
    ) {
        return new CategoryMapping(lclsCode, contentTypeId, appCategory, priority, version);
    }
}
