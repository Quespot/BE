package com.quespot.domain.mission.entity;

import com.quespot.domain.mission.enums.MissionAttemptStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

// active_key는 MySQL에 부분 유니크 인덱스가 없어서 쓰는 생성 컬럼 패턴이다
// (CLAUDE.md에 이미 정의됨). Hibernate가 생성 컬럼 자체를 만들지 못하므로,
// 여기서는 읽기 전용(insertable/updatable=false)으로만 매핑하고, 최초 배포 시
// 아래 ALTER를 로컬/CI/운영 각 환경에 한 번 수동 적용해야 한다:
//
//   ALTER TABLE mission_attempts
//     MODIFY COLUMN active_key VARCHAR(64) GENERATED ALWAYS AS (
//       CASE WHEN status = 'IN_PROGRESS' THEN CONCAT(user_id, ':', mission_id) END
//     ) STORED;
//
// (uk_attempt_active 유니크 인덱스 자체는 @UniqueConstraint로 Hibernate가
// 테이블 생성 시 같이 만들어주므로 ALTER에는 ADD UNIQUE KEY가 필요 없다.)
@Entity
@Table(
        name = "mission_attempts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attempt_active", columnNames = "active_key")
        },
        indexes = {
                @Index(name = "ix_attempt_user_status", columnList = "user_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionAttempt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    // 코스는 별도 이슈 범위. 참조할 course_attempts 테이블이 아직 없어 FK는
    // 만들지 않는다. 컬럼만 미리 둔다 — 데이터가 쌓인 뒤 ALTER 하는 것보다 싸다.
    @Column(name = "course_attempt_id")
    private Long courseAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MissionAttemptStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "arrival_latitude", precision = 13, scale = 10)
    private BigDecimal arrivalLatitude;

    @Column(name = "arrival_longitude", precision = 13, scale = 10)
    private BigDecimal arrivalLongitude;

    @Column(name = "earned_point")
    private Integer earnedPoint;

    @Column(name = "reflection", columnDefinition = "TEXT")
    private String reflection;

    @Column(name = "active_key", insertable = false, updatable = false, length = 64)
    private String activeKey;

    private MissionAttempt(Long userId, Mission mission) {
        this.userId = userId;
        this.mission = mission;
        this.status = MissionAttemptStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public static MissionAttempt start(Long userId, Mission mission) {
        return new MissionAttempt(userId, mission);
    }

    public void complete(BigDecimal arrivalLatitude, BigDecimal arrivalLongitude, Integer earnedPoint) {
        this.status = MissionAttemptStatus.COMPLETED;
        this.arrivalLatitude = arrivalLatitude;
        this.arrivalLongitude = arrivalLongitude;
        this.earnedPoint = earnedPoint;
        this.completedAt = LocalDateTime.now();
    }

    public void quit() {
        this.status = MissionAttemptStatus.QUIT;
    }

    public void writeReflection(String content) {
        this.reflection = content;
    }
}
