package com.quespot.domain.tour.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tour_contents_raw",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_raw_content",
                        columnNames = {"content_id", "operation", "api_modified_time"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TourContentRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "content_id", nullable = false, length = 20)
    private String contentId;

    @Column(name = "operation", nullable = false, length = 30)
    private String operation;

    // Map으로 매핑하면 Jackson 재직렬화로 원본이 변형된다. 이 테이블의 존재 이유가
    // 원본 보존이라 파싱하지 않고 문자열 그대로 저장·조회한다.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "json")
    private String payload;

    @Column(name = "api_modified_time", nullable = false)
    private LocalDateTime apiModifiedTime;

    @Column(name = "show_flag", nullable = false)
    private Boolean showFlag;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    private TourContentRaw(
            String contentId,
            String operation,
            String payload,
            LocalDateTime apiModifiedTime,
            Boolean showFlag
    ) {
        this.contentId = contentId;
        this.operation = operation;
        this.payload = payload;
        this.apiModifiedTime = apiModifiedTime;
        this.showFlag = showFlag;
        this.fetchedAt = LocalDateTime.now();
    }

    public static TourContentRaw create(
            String contentId,
            String operation,
            String payload,
            LocalDateTime apiModifiedTime,
            Boolean showFlag
    ) {
        return new TourContentRaw(contentId, operation, payload, apiModifiedTime, showFlag);
    }
}
