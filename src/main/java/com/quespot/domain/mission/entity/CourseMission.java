package com.quespot.domain.mission.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// course_missions는 등록 후 안 바뀌는 순수 연결 테이블이라 감사 컬럼(created_at/
// updated_at)이 없다 — BaseEntity를 상속하지 않는다. 정적 팩토리도 없음(SQL 등록).
@Entity
@Table(
        name = "course_missions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_course_seq", columnNames = {"course_id", "seq"}),
                @UniqueConstraint(name = "uk_course_mission", columnNames = {"course_id", "mission_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private MissionCourse course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(name = "seq", nullable = false)
    private Integer seq;
}
