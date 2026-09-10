package com.quespot.domain.spot.entity;

import com.quespot.domain.spot.enums.AppCategory;
import com.quespot.domain.spot.enums.SpotSource;
import com.quespot.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "spots",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_spots_source", columnNames = {"source", "source_content_id"})
        },
        indexes = {
                @Index(name = "ix_spots_geo", columnList = "latitude, longitude"),
                @Index(name = "ix_spots_area", columnList = "ldong_regn_cd, ldong_signgu_cd, app_category")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Spot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private SpotSource source;

    @Column(name = "source_content_id", length = 20)
    private String sourceContentId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "address")
    private String address;

    @Column(name = "latitude", nullable = false, precision = 13, scale = 10)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 13, scale = 10)
    private BigDecimal longitude;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "kto_content_type_id")
    private Integer ktoContentTypeId;

    // 추천코스 계열(AC/AC01/AC010100, C01/C0112/C01120001)이 다른 분류보다 한 자리씩
    // 길어서 CHAR가 아닌 VARCHAR(3/5/9)로 둔다. docs/quespot_schema.sql 상단 설계 메모 참고.
    @Column(name = "lcls_systm1", length = 3)
    private String lclsSystm1;

    @Column(name = "lcls_systm2", length = 5)
    private String lclsSystm2;

    @Column(name = "lcls_systm3", length = 9)
    private String lclsSystm3;

    @Column(name = "ldong_regn_cd", columnDefinition = "CHAR(2)")
    private String ldongRegnCd;

    @Column(name = "ldong_signgu_cd", columnDefinition = "CHAR(3)")
    private String ldongSignguCd;

    @Column(name = "district_code", length = 10)
    private String districtCode;

    // Type3는 이미지 크롭·썸네일 재생성이 저작권 위반이라 firstimage2를 그대로 써야 한다.
    // 정제/이미지 처리 배치가 가공 가부를 판단할 때 이 값을 본다.
    @Column(name = "cpyrht_div_cd", length = 10)
    private String cpyrhtDivCd;

    // 값 집합이 고정이고 화면 필터 탭이 이걸로 그려진다. 정제 배치에서 "HISTROY" 같은
    // 오타가 컴파일에서 잡혀야 해서 String이 아니라 enum이다.
    @Enumerated(EnumType.STRING)
    @Column(name = "app_category", nullable = false, length = 20)
    private AppCategory appCategory;

    // category_mappings 룰 버전이 올라가면 이 값보다 낮은 스팟을 재정제 대상으로 추적한다.
    @Column(name = "category_mapping_version")
    private Integer categoryMappingVersion;

    // 표출 여부 · 삭제 대신 플래그. 정제 배치(SpotRefinementWriter)가 원본 showflag
    // 값을 그대로 반영한다 — false여도 삭제하지 않고 플래그만 내린다.
    @Column(name = "show_flag", nullable = false)
    private Boolean showFlag;

    @Column(name = "source_modified_at")
    private LocalDateTime sourceModifiedAt;

    @Builder
    private Spot(
            SpotSource source,
            String sourceContentId,
            String name,
            String summary,
            String description,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String imageUrl,
            String thumbnailUrl,
            Integer ktoContentTypeId,
            String lclsSystm1,
            String lclsSystm2,
            String lclsSystm3,
            String ldongRegnCd,
            String ldongSignguCd,
            String districtCode,
            String cpyrhtDivCd,
            AppCategory appCategory,
            Integer categoryMappingVersion,
            Boolean showFlag,
            LocalDateTime sourceModifiedAt
    ) {
        this.source = source;
        this.sourceContentId = sourceContentId;
        this.name = name;
        this.summary = summary;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.ktoContentTypeId = ktoContentTypeId;
        this.lclsSystm1 = lclsSystm1;
        this.lclsSystm2 = lclsSystm2;
        this.lclsSystm3 = lclsSystm3;
        this.ldongRegnCd = ldongRegnCd;
        this.ldongSignguCd = ldongSignguCd;
        this.districtCode = districtCode;
        this.cpyrhtDivCd = cpyrhtDivCd;
        this.appCategory = appCategory;
        this.categoryMappingVersion = categoryMappingVersion;
        this.showFlag = showFlag;
        this.sourceModifiedAt = sourceModifiedAt;
    }

    // 정제 배치가 같은 content_id를 다시 정제할 때(원천이 갱신됐을 때) 호출한다.
    // freshData는 Spot.builder()로 새로 지은 "다음 상태"이고, 같은 클래스라 private
    // 필드에 직접 접근할 수 있다. id/source/sourceContentId(정체성)와
    // summary/description(areaBasedSyncList2 응답에 없어 이 배치가 안 채우는 필드)은
    // 건드리지 않는다.
    public void refresh(Spot freshData) {
        this.name = freshData.name;
        this.address = freshData.address;
        this.latitude = freshData.latitude;
        this.longitude = freshData.longitude;
        this.imageUrl = freshData.imageUrl;
        this.thumbnailUrl = freshData.thumbnailUrl;
        this.ktoContentTypeId = freshData.ktoContentTypeId;
        this.lclsSystm1 = freshData.lclsSystm1;
        this.lclsSystm2 = freshData.lclsSystm2;
        this.lclsSystm3 = freshData.lclsSystm3;
        this.ldongRegnCd = freshData.ldongRegnCd;
        this.ldongSignguCd = freshData.ldongSignguCd;
        this.districtCode = freshData.districtCode;
        this.cpyrhtDivCd = freshData.cpyrhtDivCd;
        this.appCategory = freshData.appCategory;
        this.categoryMappingVersion = freshData.categoryMappingVersion;
        this.showFlag = freshData.showFlag;
        this.sourceModifiedAt = freshData.sourceModifiedAt;
    }
}
