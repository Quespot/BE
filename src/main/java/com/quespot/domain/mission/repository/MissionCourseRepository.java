package com.quespot.domain.mission.repository;

import com.quespot.domain.mission.entity.MissionCourse;
import com.quespot.domain.mission.enums.MissionCourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MissionCourseRepository extends JpaRepository<MissionCourse, Long> {

    Optional<MissionCourse> findByIdAndStatus(Long id, MissionCourseStatus status);

    List<MissionCourse> findByCreatedByUserIdOrderByCreatedAtDesc(Long createdByUserId);

    // TODO(#39 Task 10에서 제거): MissionCourseQueryService.getCourses가 아직 이
    // 두 메서드를 쓴다 — Task 10에서 getCourses를 findByCreatedByUserIdOrderByCreatedAtDesc
    // 기반으로 바꾸면서 같이 지운다. 지금 지우면 컴파일이 깨진다.
    List<MissionCourse> findByStatus(MissionCourseStatus status);

    List<MissionCourse> findByRegionCodeAndStatus(String regionCode, MissionCourseStatus status);
}
