package com.quespot.domain.mission.entity;

import com.quespot.domain.mission.enums.CourseAttemptStatus;
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

// active_key는 mission_attempts와 완전히 같은 패턴이다(#37). MySQL에 부분
// 유니크 인덱스가 없어서 생성 컬럼으로 진행 중 중복 시작을 막는다. Hibernate는
// 생성 컬럼을 만들지 못하므로 읽기 전용으로만 매핑하고, 최초 배포 시 아래
// ALTER를 로컬/CI/운영 각 환경에 한 번 수동 적용해야 한다:
//
//   ALTER TABLE course_attempts
//     MODIFY COLUMN active_key VARCHAR(64) GENERATED ALWAYS AS (
//       CASE WHEN status = 'IN_PROGRESS' THEN CONCAT(user_id, ':', course_id) END
//     ) STORED;
//
// (uk_course_attempt_active 유니크 인덱스는 @UniqueConstraint로 Hibernate가
// 테이블 생성 시 같이 만들어주므로 ALTER에 ADD UNIQUE KEY는 필요 없다.)
//
// QUIT 시점을 담는 별도 컬럼은 없다 — updated_at(BaseEntity)으로 대신
// 확인한다(mission_attempts와 동일한 판단, #37).
@Entity
@Table(
        name = "course_attempts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_course_attempt_active", columnNames = "active_key")
        },
        indexes = {
                @Index(name = "ix_course_attempt_user_status", columnList = "user_id, status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseAttempt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private MissionCourse course;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CourseAttemptStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "earned_bonus_point")
    private Integer earnedBonusPoint;

    @Column(name = "active_key", insertable = false, updatable = false, length = 64)
    private String activeKey;

    private CourseAttempt(Long userId, MissionCourse course) {
        this.userId = userId;
        this.course = course;
        this.status = CourseAttemptStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public static CourseAttempt start(Long userId, MissionCourse course) {
        return new CourseAttempt(userId, course);
    }

    public void complete(Integer bonusPoint) {
        this.status = CourseAttemptStatus.COMPLETED;
        this.earnedBonusPoint = bonusPoint;
        this.completedAt = LocalDateTime.now();
    }

    public void quit() {
        this.status = CourseAttemptStatus.QUIT;
    }
}
